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

## 🚀 Instrucciones de Instalación

1. Asegúrate de tener instalado **Docker** y **Docker Compose** en tu máquina.
2. Abre una terminal en la raíz del proyecto.
3. Ejecuta el siguiente comando para construir la imagen del backend y levantar toda la infraestructura:
   ```bash
   docker-compose up -d --build
   ```
4. Los servicios quedarán así:
   - PostgreSQL: `localhost:5432` (`POSTGRES_USER=admin`, `POSTGRES_PASSWORD=admin`)
   - MySQL: `localhost:3306` (`root`, `MYSQL_ROOT_PASSWORD=admin`)
   - Backend: `http://localhost:8080` (corriendo desde la imagen `bank-backend`).
5. Verifica logs y salud:
   - `docker logs banco_nacional_db`
   - `docker logs banco_internacional_db`
   - `docker logs bank-backend`
6. Las propiedades de conexión se pueden sobreescribir vía variables de entorno (ya preparadas en `docker-compose.yml`):
   - `SPRING_DATASOURCE_NACIONAL_URL`, `SPRING_DATASOURCE_NACIONAL_USERNAME`, `SPRING_DATASOURCE_NACIONAL_PASSWORD`
   - `SPRING_DATASOURCE_INTERNACIONAL_URL`, `SPRING_DATASOURCE_INTERNACIONAL_USERNAME`, `SPRING_DATASOURCE_INTERNACIONAL_PASSWORD`
7. Revisa los logs en nivel `DEBUG` para validar las conexiones JPA exitosas y confirmar que ambos DataSources están activos antes de implementar las operaciones SAGA.
8. Abre el index.html en tu navegador

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
