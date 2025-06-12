# --- Faza 1: Construirea Frontend-ului ---
# Folosim o imagine oficială Node.js pentru a construi partea de React
FROM node:18-alpine AS frontend_build

# Setăm directorul de lucru
WORKDIR /app/frontend

# Copiem doar fișierele de configurare pentru a beneficia de cache-ul Docker
COPY frontend/package.json frontend/package-lock.json ./

# Instalăm dependențele frontend
RUN npm install

# Copiem restul codului sursă frontend
COPY frontend/ ./

# Rulăm comanda de build pentru React
RUN npm run build

# --- Faza 2: Construirea Backend-ului ---
# Acum folosim imaginea Gradle pentru a construi aplicația Java
FROM gradle:jdk17-focal AS backend_build

# Setăm directorul de lucru
WORKDIR /home/gradle/src

# Copiem tot proiectul în container
COPY --chown=gradle:gradle . .

# IMPORTANT: Copiem frontend-ul deja construit din faza anterioară
# în locația unde se așteaptă Spring Boot să-l găsească
COPY --from=frontend_build /app/frontend/build ./src/main/resources/static/

# Construim proiectul Gradle, dar excludem (-x) task-urile de frontend
# deoarece le-am rulat deja în faza anterioară.
RUN gradle build -x test -x buildReact -x copyReact --no-daemon

# --- Faza 3: Rularea Aplicației Finale ---
# Folosim o imagine mică, doar cu Java, pentru a rula aplicația
FROM eclipse-temurin:17-jre-jammy

# Creăm un user non-root pentru securitate
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --gid 1001 appuser

# Expunem portul pe care Render se așteaptă să ruleze serviciul
# Render utilizează variabila de mediu PORT (de obicei 8080)
EXPOSE $PORT

# Copiem doar fișierul .jar final din faza de build a backend-ului
COPY --from=backend_build /home/gradle/src/build/libs/*.jar app.jar

# Schimbăm ownership-ul fișierului
RUN chown appuser:appgroup app.jar

# Comutăm la user-ul non-root
USER appuser

# Comanda de pornire cu configurările necesare pentru Render
ENTRYPOINT ["java", \
    "-server", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dserver.port=${PORT:-8080}", \
    "-Dserver.address=0.0.0.0", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-production}", \
    "-jar", \
    "app.jar"]