# ESCUDO IA · LIVEL V

MVP CyberTech para evaluar riesgo en mensajes, enlaces y capturas. Entrega un score 0-100, indicadores explicables y una acción prudente. El producto evalúa riesgo: no garantiza seguridad.

## Alcance implementado

- Registro, login, JWT de corta duración y refresh tokens rotables/revocables.
- Roles `USER`, `ADMIN` y `SUPER_ADMIN`, bootstrap seguro del primer administrador.
- Quick Scan público y análisis autenticado de texto, URL y capturas con OCR local en español e inglés.
- Risk Engine determinístico, versionado y configurable desde ADMIN.
- Redacción de correo, teléfono, tarjeta, CUIT/CUIL y OTP antes de la capa IA mock.
- Las capturas y su texto OCR no se guardan; sólo se persisten el resultado y sus indicadores.
- URLs normalizadas sin fetch activo; bloqueo explícito de destinos privados/locales.
- Historial con ownership, cuota diaria, request ID, rate limit y auditoría con IP/User-Agent hasheados.
- Planes/precios en PostgreSQL, checkout único y suscripción de Mercado Pago, webhook idempotente y verificación por API.
- React/Vite responsive, PWA instalable, estados de carga/error/vacío/éxito.
- Flyway, tests de auth/Risk Engine, Docker y Blueprint de Render.

## Arquitectura

Monolito modular Spring Boot 3 / Java 21 que sirve el build de React en producción. PostgreSQL es el único servicio obligatorio. Los adaptadores `AIProvider`, `ThreatIntelProvider` y `PaymentProvider` evitan acoplar el dominio a proveedores externos.

El proceso principal **no visita URLs enviadas por usuarios**. Un scanner activo futuro debe desplegarse con otra frontera de red, sin acceso a DB, secretos ni red privada.

## Desarrollo local

Requisitos: Java 21+, Maven 3.9+, Node 22+, Tesseract con datos `spa+eng` y Docker para PostgreSQL. El contenedor de producción ya incluye Tesseract.

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run
```

En otra terminal:

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`. API/health: `http://localhost:8080/actuator/health`.

Para usar configuración propia, copiá `.env.example` a un archivo local ignorado por Git y exportá sus valores en tu terminal o IDE.

## Pruebas y builds

```bash
cd backend && mvn test
cd frontend && npm run build
docker build -t escudo-ia .
```

Las pruebas usan H2 en modo PostgreSQL y Mercado Pago mock; nunca consumen credenciales reales.

## Deploy manual en Render (Web Service)

1. Subí el repositorio a GitHub o GitLab.
2. Abrí tu PostgreSQL Free existente y copiá los datos de `Connect`. Usá la conexión interna y la misma región para el Web Service.
3. En Render elegí **New → Web Service** y conectá el repositorio; no crees otro PostgreSQL ni uses el Blueprint para provisionar la base.
4. Elegí `Docker`, plan `Free`, Dockerfile `./Dockerfile` y Health Check Path `/actuator/health`.
5. En **Environment**, cargá las variables de `render.env.example`. Completá `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` con los datos de tu base existente.
6. Cargá también las credenciales de Mercado Pago y las variables `ADMIN_EMAIL` y `ADMIN_PASSWORD`.
7. Creá el Web Service. Render asignará automáticamente una URL gratuita `https://<servicio>.onrender.com`.
8. En Mercado Pago registrá `https://<tu-dominio-onrender>/api/pagos/webhook` y copiá la firma secreta a `MERCADOPAGO_WEBHOOK_SECRET`.
9. Verificá `/actuator/health`, creá una cuenta, ejecutá un scan y completá un pago de prueba.

No hace falta definir `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` ni `MP_WEBHOOK_URL`: la aplicación usa automáticamente `RENDER_EXTERNAL_URL`. El `render.yaml` queda como alternativa de Web Service solamente y ya no intenta crear una base.

El webhook no confía en el redirect del navegador: consulta el pago a Mercado Pago antes de confirmar. Si se configuró `MERCADOPAGO_WEBHOOK_SECRET`, también valida `x-signature`.

El Blueprint usa instancias gratuitas para la primera prueba. Render indica que PostgreSQL Free expira a los 30 días y no incluye backups; antes de publicar comercialmente, cambiá la base a un plan pago con recuperación.

## Variables importantes

La lista completa está en `.env.example`. En producción son obligatorios una clave JWT aleatoria de 32+ bytes, PostgreSQL, los orígenes CORS explícitos y las credenciales del proveedor de pagos. No hay secretos versionados.

## Próxima fase

- Complemento multimodal del OCR para detectar señales visuales sin almacenar las capturas.
- Threat Intelligence real y scanner aislado con egress controlado.
- Activación efectiva de beneficios de suscripción, cancelaciones contra Mercado Pago y cuotas por plan.
- Informe PDF, Family, modo incidente, notificaciones y métricas comerciales.
