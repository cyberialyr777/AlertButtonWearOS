# 🚨 AlertButton WearOS - Sincronización con Backend

## 📋 Resumen de Funcionalidades Implementadas

### ✅ **Vistas Creadas:**
1. **AuthScreen** - Pantalla de autenticación
2. **ContactsScreen** - Visualización de contactos de emergencia
3. **EmergencyScreen** - Pantalla principal con botón SOS (actualizada)
4. **SuccessScreen** - Confirmación de alerta enviada (actualizada)

### ✅ **Modelos de Datos:**
- `User` - Información del usuario
- `EmergencyContact` - Contactos de emergencia
- `Device` - Información del dispositivo
- `UserData` - Datos completos del usuario
- `AuthResponse` - Respuesta de autenticación
- `EmergencyAlert` - Alerta con contactos incluidos
- `AlertResponse` - Respuesta del servidor

### ✅ **Servicios:**
- `BackendService` - Manejo completo del backend
- `ApiService` - Interfaz de API REST
- `SharedPrefs` - Gestión de tokens y datos locales

## 🔧 Configuración del Backend

### 1. **Estructura de Base de Datos (PostgreSQL)**

```sql
-- Tabla de usuarios
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Tabla de dispositivos
CREATE TABLE user_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    device_type VARCHAR(50) NOT NULL, -- 'mobile' o 'wearable'
    device_id VARCHAR(255) UNIQUE NOT NULL,
    device_name VARCHAR(255),
    last_sync TIMESTAMP DEFAULT NOW(),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabla de contactos de emergencia
CREATE TABLE emergency_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Tabla de alertas de emergencia
CREATE TABLE emergency_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    timestamp TIMESTAMP DEFAULT NOW(),
    status VARCHAR(50) DEFAULT 'sent', -- 'sent', 'received', 'responded'
    contacts_notified INTEGER DEFAULT 0,
    response_data JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Índices para mejor rendimiento
CREATE INDEX idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX idx_emergency_contacts_user_id ON emergency_contacts(user_id);
CREATE INDEX idx_emergency_alerts_user_id ON emergency_alerts(user_id);
CREATE INDEX idx_emergency_alerts_timestamp ON emergency_alerts(timestamp);
```

### 2. **API Endpoints (Node.js + Express)**

```javascript
// server.js
const express = require('express');
const cors = require('cors');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json());

// Configuración de base de datos
const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: { rejectUnauthorized: false }
});

// Middleware de autenticación
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    
    if (!token) {
        return res.status(401).json({ error: 'Token requerido' });
    }
    
    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
        if (err) return res.status(403).json({ error: 'Token inválido' });
        req.user = user;
        next();
    });
};

// 1. Autenticación
app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        
        const result = await pool.query(
            'SELECT * FROM users WHERE email = $1',
            [email]
        );
        
        if (result.rows.length === 0) {
            return res.status(401).json({ error: 'Credenciales inválidas' });
        }
        
        const user = result.rows[0];
        const validPassword = await bcrypt.compare(password, user.password_hash);
        
        if (!validPassword) {
            return res.status(401).json({ error: 'Credenciales inválidas' });
        }
        
        const accessToken = jwt.sign(
            { userId: user.id, email: user.email },
            process.env.JWT_SECRET,
            { expiresIn: '1h' }
        );
        
        const refreshToken = jwt.sign(
            { userId: user.id },
            process.env.REFRESH_SECRET,
            { expiresIn: '7d' }
        );
        
        res.json({
            user: {
                id: user.id,
                email: user.email,
                name: user.name,
                phone: user.phone
            },
            accessToken,
            refreshToken
        });
    } catch (error) {
        console.error('Error en login:', error);
        res.status(500).json({ error: 'Error interno del servidor' });
    }
});

// 2. Registro de dispositivos
app.post('/api/devices/register', authenticateToken, async (req, res) => {
    try {
        const { deviceType, deviceId, deviceName } = req.body;
        const userId = req.user.userId;
        
        const result = await pool.query(
            `INSERT INTO user_devices (user_id, device_type, device_id, device_name)
             VALUES ($1, $2, $3, $4)
             ON CONFLICT (device_id) 
             DO UPDATE SET 
                device_name = $4,
                last_sync = NOW(),
                is_active = true
             RETURNING *`,
            [userId, deviceType, deviceId, deviceName]
        );
        
        res.json(result.rows[0]);
    } catch (error) {
        console.error('Error al registrar dispositivo:', error);
        res.status(500).json({ error: 'Error interno del servidor' });
    }
});

// 3. Obtener datos del usuario
app.get('/api/sync/user/:userId', authenticateToken, async (req, res) => {
    try {
        const userId = req.params.userId;
        
        // Verificar que el usuario autenticado accede a sus propios datos
        if (req.user.userId !== userId) {
            return res.status(403).json({ error: 'Acceso denegado' });
        }
        
        const [userResult, contactsResult, devicesResult] = await Promise.all([
            pool.query('SELECT id, email, name, phone FROM users WHERE id = $1', [userId]),
            pool.query('SELECT * FROM emergency_contacts WHERE user_id = $1 AND is_active = true', [userId]),
            pool.query('SELECT * FROM user_devices WHERE user_id = $1 AND is_active = true', [userId])
        ]);
        
        if (userResult.rows.length === 0) {
            return res.status(404).json({ error: 'Usuario no encontrado' });
        }
        
        res.json({
            user: userResult.rows[0],
            contacts: contactsResult.rows,
            devices: devicesResult.rows
        });
    } catch (error) {
        console.error('Error al obtener datos del usuario:', error);
        res.status(500).json({ error: 'Error interno del servidor' });
    }
});

// 4. Obtener contactos del usuario
app.get('/api/contacts/user/:userId', authenticateToken, async (req, res) => {
    try {
        const userId = req.params.userId;
        
        if (req.user.userId !== userId) {
            return res.status(403).json({ error: 'Acceso denegado' });
        }
        
        const result = await pool.query(
            'SELECT * FROM emergency_contacts WHERE user_id = $1 AND is_active = true',
            [userId]
        );
        
        res.json(result.rows);
    } catch (error) {
        console.error('Error al obtener contactos:', error);
        res.status(500).json({ error: 'Error interno del servidor' });
    }
});

// 5. Enviar alerta de emergencia
app.post('/api/alerts/emergency', authenticateToken, async (req, res) => {
    try {
        const { latitude, longitude, userId, deviceId, contacts } = req.body;
        
        if (req.user.userId !== userId) {
            return res.status(403).json({ error: 'Acceso denegado' });
        }
        
        // Guardar la alerta
        const alertResult = await pool.query(
            `INSERT INTO emergency_alerts (user_id, device_id, latitude, longitude)
             VALUES ($1, $2, $3, $4)
             RETURNING id`,
            [userId, deviceId, latitude, longitude]
        );
        
        const alertId = alertResult.rows[0].id;
        let contactsNotified = 0;
        
        // Notificar a contactos (simulado)
        if (contacts && contacts.length > 0) {
            // Aquí implementarías la lógica real de notificación
            // Por ejemplo: SMS, email, push notifications
            contactsNotified = contacts.length;
            
            console.log(`Alerta ${alertId}: Notificando a ${contactsNotified} contactos`);
            
            // Simular envío de SMS/email
            for (const contact of contacts) {
                console.log(`Enviando SMS a ${contact.phone} - ${contact.name}`);
                // Implementar envío real de SMS aquí
            }
        }
        
        // Actualizar número de contactos notificados
        await pool.query(
            'UPDATE emergency_alerts SET contacts_notified = $1 WHERE id = $2',
            [contactsNotified, alertId]
        );
        
        res.json({
            success: true,
            message: 'Alerta enviada exitosamente',
            alertId: alertId,
            contactsNotified: contactsNotified
        });
    } catch (error) {
        console.error('Error al enviar alerta:', error);
        res.status(500).json({ error: 'Error interno del servidor' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Servidor corriendo en puerto ${PORT}`);
});
```

### 3. **Variables de Entorno (.env)**

```env
DATABASE_URL=postgresql://usuario:password@localhost:5432/alertas_db
JWT_SECRET=tu_jwt_secret_super_seguro
REFRESH_SECRET=tu_refresh_secret_super_seguro
PORT=3000
```

## 📱 Configuración en la App WearOS

### 1. **Actualizar URL del Backend**

En `BackendConfig.kt`, cambiar la URL:

```kotlin
const val BASE_URL = "https://tu-backend-real.com/api/"
```

### 2. **Implementar SharedPreferences**

Completar la implementación en `SharedPrefs.kt`:

```kotlin
object SharedPrefs {
    private const val PREF_NAME = "AlertButtonPrefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    
    private fun getSharedPreferences(context: Context) = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    fun saveAccessToken(context: Context, token: String) {
        getSharedPreferences(context).edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }
    
    fun getAccessToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_ACCESS_TOKEN, null)
    }
    
    fun saveRefreshToken(context: Context, token: String) {
        getSharedPreferences(context).edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }
    
    fun getRefreshToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_REFRESH_TOKEN, null)
    }
    
    fun saveUserId(context: Context, userId: String) {
        getSharedPreferences(context).edit().putString(KEY_USER_ID, userId).apply()
    }
    
    fun getUserId(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_USER_ID, null)
    }
    
    fun clearAll(context: Context) {
        getSharedPreferences(context).edit().clear().apply()
    }
}
```

## 🔄 Flujo de Sincronización

### 1. **Primera vez que se abre la app:**
- Mostrar pantalla de autenticación
- Usuario puede hacer login o saltar
- Si hace login, se registra el dispositivo WearOS
- Se sincronizan contactos y configuraciones

### 2. **Al presionar el botón SOS:**
- Obtener ubicación actual
- Obtener contactos del usuario desde el backend
- Enviar alerta con ubicación + contactos
- El backend notifica a todos los contactos
- Mostrar confirmación con detalles

### 3. **Sincronización automática:**
- Al abrir la app, verificar si hay datos nuevos
- Actualizar contactos si han cambiado
- Mantener tokens de autenticación actualizados

## 🚀 Despliegue del Backend

### Opción 1: Heroku
```bash
# Crear app en Heroku
heroku create tu-app-alertas

# Configurar base de datos PostgreSQL
heroku addons:create heroku-postgresql:hobby-dev

# Configurar variables de entorno
heroku config:set JWT_SECRET=tu_secret_super_seguro
heroku config:set REFRESH_SECRET=tu_refresh_secret

# Desplegar
git push heroku main
```

### Opción 2: Railway
```bash
# Conectar repositorio a Railway
# Configurar variables de entorno en el dashboard
# Railway detectará automáticamente el Node.js app
```

### Opción 3: VPS propio
```bash
# Instalar Node.js y PostgreSQL
# Configurar nginx como proxy reverso
# Usar PM2 para mantener la app corriendo
pm2 start server.js --name "alertas-backend"
```

## 📊 Monitoreo y Logs

### Logs importantes a monitorear:
- Intentos de login fallidos
- Alertas enviadas
- Contactos notificados
- Errores de sincronización
- Dispositivos registrados

### Métricas útiles:
- Número de usuarios activos
- Frecuencia de alertas
- Tiempo de respuesta del servidor
- Tasa de éxito en notificaciones

## 🔒 Seguridad

### Implementar:
- Rate limiting para prevenir spam
- Validación de entrada en todos los endpoints
- HTTPS obligatorio
- Logs de auditoría
- Backup automático de base de datos
- Monitoreo de intentos de acceso sospechosos

## 📞 Próximos Pasos

1. **Implementar el backend** siguiendo el código de ejemplo
2. **Configurar la URL** en `BackendConfig.kt`
3. **Probar la autenticación** con un usuario de prueba
4. **Configurar notificaciones reales** (SMS/email)
5. **Implementar la app móvil** complementaria
6. **Agregar más funcionalidades** como historial de alertas

¿Te gustaría que te ayude con algún paso específico o tienes alguna pregunta sobre la implementación? 