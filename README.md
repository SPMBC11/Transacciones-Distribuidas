# Transacciones Distribuidas PostgreSQL + MySQL

Este repositorio contiene la arquitectura base para la implementación de un sistema de transferencias interbancarias haciendo uso de **Spring Boot**, **PostgreSQL** y **MySQL**.

## 📌 Arquitectura Implementada

Se establece un sistema de transferencias interbancarias con separación estricta de dominios de datos:
- **Banco Nacional:** Soporte en base de datos PostgreSQL 15 (Puerto 5432).
- **Banco Internacional:** Soporte en base de datos MySQL 8 (Puerto 3306).
- **Backend:** Aplicación Spring Boot conectada a ambas bases de datos, utilizando configuración avanzada de DataSources múltiples y asegurando persistencia separada por contexto.

Las dependencias e infraestructura se encuentran contenidas para un arranque simplificado mediante contenedores Docker, ubicados todos dentro de una red virtual compartida (`bank-network`) para asegurar la correcta comunicación y resolución DNS sin exponer las bases directamente al exterior de forma innecesaria (aunque se mapean los puertos localmente para el desarrollo).

### 🗂 Estructura de Paquetes Relevante
Se separaron las responsabilidades a nivel código para aislar las interacciones con la base de datos:
- `com.bank.backend.repository.nacional`: Dedicado al EntityManagerFactory y repositorios apuntando a PostgreSQL.
- `com.bank.backend.repository.internacional`: Dedicado al EntityManagerFactory y repositorios apuntando a MySQL.

## 🚀 Instrucciones de Instalación y Ejecución

### Prerequisitos
- **Docker** y **Docker Compose** instalados en tu máquina
- Java 17 (solo para desarrollo, no necesario si usas Docker)
- Maven (solo para desarrollo, no necesario si usas Docker)

### 🎯 Inicio Rápido (Recomendado)

Hemos creado scripts automatizados para cada sistema operativo que:
1. ✅ Inician todos los servicios Docker
2. ✅ Esperan a que las bases de datos estén listas
3. ✅ Verifican que Spring Boot haya iniciado
4. ✅ Abren automáticamente la interfaz web en tu navegador

#### 🐧 Linux / macOS

```bash
./start_linux.sh
```

**Nota:** El script requiere permisos de `sudo` para ejecutar comandos de Docker.

#### 🪟 Windows

```cmd
start_windows.bat
```

**Nota:** Ejecutar como usuario normal (no requiere permisos de administrador si Docker está configurado correctamente).

---

### 🔧 Inicio Manual (Alternativa)

Si prefieres controlar cada paso manualmente:

1. **Levantar la infraestructura:**
   ```bash
   # Linux/macOS
   sudo docker compose up -d --build
   
   # Windows (PowerShell o CMD)
   docker compose up -d --build
   ```

2. **Esperar a que los servicios estén listos** (pueden tomar 30-60 segundos):
   - PostgreSQL estará disponible en el puerto **5432**
   - MySQL estará disponible en el puerto **3306**
   - Spring Boot Backend estará en **http://localhost:8080**

3. **Verificar el estado de los servicios:**
   ```bash
   docker compose ps
   ```

4. **Abrir la interfaz web:**
   - Abre el archivo `index.html` en tu navegador preferido
   - O navega a: `file:///ruta/completa/al/proyecto/index.html`

---

### 📊 Servicios Disponibles

Una vez iniciado el sistema, tendrás acceso a:

| Servicio | URL/Puerto | Credenciales |
|----------|-----------|--------------|
| **Interfaz Web** | `index.html` | - |
| **API REST** | `http://localhost:8080/api` | - |
| **PostgreSQL** (Banco Nacional) | `localhost:5432` | admin / admin |
| **MySQL** (Banco Internacional) | `localhost:3306` | root / admin |

---

### 🔍 Verificación de Logs

Para verificar que todo esté funcionando correctamente:

```bash
# Ver logs del backend
docker compose logs -f backend

# Ver logs de PostgreSQL
docker logs banco_nacional_db

# Ver logs de MySQL
docker logs banco_internacional_db

# Ver todos los logs
docker compose logs -f
```

---

### 🛑 Detener los Servicios

```bash
# Detener servicios (mantiene los datos)
docker compose down

# Detener y eliminar volúmenes (borra todos los datos)
docker compose down -v
```

---

### 🔄 Reiniciar desde Cero

Si quieres resetear todas las bases de datos y empezar de nuevo:

```bash
# Linux/macOS
sudo docker compose down -v && sudo docker compose up -d --build

# Windows
docker compose down -v && docker compose up -d --build
```

---

### 🌐 Endpoints API Disponibles

#### Consultas de Cuentas
- `GET /api/cuentas/nacional/{numeroCuenta}` - Información completa de cuenta nacional
- `GET /api/cuentas/nacional/{numeroCuenta}/saldo` - Saldo de cuenta nacional
- `GET /api/cuentas/nacional/{numeroCuenta}/movimientos` - Movimientos de cuenta nacional
- `GET /api/cuentas/internacional/{numeroCuenta}` - Información completa de cuenta internacional
- `GET /api/cuentas/internacional/{numeroCuenta}/saldo` - Saldo de cuenta internacional
- `GET /api/cuentas/internacional/{numeroCuenta}/movimientos` - Movimientos de cuenta internacional

#### Transferencias Interbancarias
- `POST /api/transferencias/nacional-a-internacional` - Transferencia de Banco Nacional → Banco Internacional
- `POST /api/transferencias/internacional-a-nacional` - Transferencia de Banco Internacional → Banco Nacional

**Ejemplo de request:**
```json
{
  "cuentaOrigen": "BN-001",
  "cuentaDestino": "BI-001",
  "monto": 1000.00
}
```

---

### 👥 Cuentas de Prueba

#### Banco Nacional (PostgreSQL)
- **BN-001** - Juan Pérez (Saldo inicial: $5,000)
- **BN-002** - María García (Saldo inicial: $10,000)
- **BN-003** - Carlos Rodríguez (Saldo inicial: $15,000)
- **BN-004** - Ana Martínez (Saldo inicial: $7,500)

#### Banco Internacional (MySQL)
- **BI-001** - Laura Sánchez (Saldo inicial: $8,000)
- **BI-002** - Pedro López (Saldo inicial: $3,000)
- **BI-003** - Sofia Hernández (Saldo inicial: $12,000)
- **BI-004** - Diego Torres (Saldo inicial: $6,000)

---

### ⚙️ Configuración Avanzada

Las propiedades de conexión se pueden sobreescribir vía variables de entorno (ya preparadas en `docker-compose.yml`):
- `SPRING_DATASOURCE_NACIONAL_URL`, `SPRING_DATASOURCE_NACIONAL_USERNAME`, `SPRING_DATASOURCE_NACIONAL_PASSWORD`
- `SPRING_DATASOURCE_INTERNACIONAL_URL`, `SPRING_DATASOURCE_INTERNACIONAL_USERNAME`, `SPRING_DATASOURCE_INTERNACIONAL_PASSWORD`

Los logs están configurados en nivel `DEBUG` para validar las conexiones JPA exitosas y confirmar que ambos DataSources están activos.

## ⚖️ Decisiones y Enfoque Arquitectónico (Trade-offs)

### ¿Por qué no se usa XA / 2PC (Two-Phase Commit)?
El protocolo *Two-Phase Commit (2PC)* obliga a un bloqueo temporal sincrónico (Coordinador bloquea todos los recursos hasta que todos validen). En un contexto de bancos diferentes e independientes, generar bloqueos *cross-database* introduce latencia inaceptable, acoplamiento alto, baja tolerancia a caídas prolongadas de nodos, y bloqueos de red perjudiciales que degradan el throughput dramáticamente. Además, algunos drivers y bases de datos modernas limitan o desaconsejan soporte XA estricto por rendimiento.

### ¿Por qué se implementa SAGA?
El patrón **SAGA**, especialmente en su variante orquestada o coreografiada, resuelve las transacciones distribuidas como una secuencia de transacciones *locales*, que son inmediatamente commiteadas. 
- Permite que cada banco persista sus transacciones en sus bases de datos (PostgreSQL para Nacional, MySQL para Internacional) de manera independiente.
- Si un paso del SAGA falla (ej. el banco destino rechaza la transacción por fondos o la cuenta no existe), el sistema ejecuta **transacciones compensatorias** sobre el banco origen para revertir lógicamente el saldo (ej. se deposita de vuelta el dinero al origen).
- Fomenta sistemas reactivos, tolerantes a fallos (disponibilidad) y evita bloqueos largos de base de datos.

### Trade-offs entre Consistencia y Disponibilidad
- **Teorema CAP:** Hemos preferido un modelo orientado a **Eventual Consistency** (Consistencia Eventual) y alta **Disponibilidad**. 
- Bajo 2PC garantizamos "ACID fuerte" a nivel global (Consistency), sacrificando fuertemente Availability si una BD de la red está lenta o caída.
- Bajo SAGA hay picos de inconsistencia semántica muy breves (el dinero sale del Banco Nacional y puede tomar un instante hasta procesarse y acreditarse en el Banco Internacional). Sin embargo, mantenemos alta disponibilidad para procesamiento de ambas bases y manejamos de forma segura las confirmaciones/compensaciones, favoreciendo el comportamiento real en sistemas bancarios y microservicios modernos.

### Separación de Responsabilidades
Para soportar SAGA dentro de la misma capa de aplicación o en microservicios, el primer paso es **aislar el stack de datos**. Cada Base de Datos tiene su propia configuración `@Configuration`, su propio DataSource (`NacionalDataSourceConfig`, `InternacionalDataSourceConfig`), su propio `EntityManagerFactory` y su gestor de transacciones independiente. Al separar los paquetes `repository.nacional` e `repository.internacional`, nos aseguramos que una transacción mal armada no use accidentalmente la conexión del otro banco, pilar fundamental para luego construir un servicio orquestador confiable.
