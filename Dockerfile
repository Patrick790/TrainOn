# --- Faza 1: Construirea aplicației folosind Gradle ---
# Folosim o imagine oficială Gradle cu JDK 17
FROM gradle:jdk17-focal AS build

# Setăm directorul de lucru în container
WORKDIR /home/gradle/src

# Copiem tot codul sursă în container
COPY --chown=gradle:gradle . .

# Rulăm comanda de build pentru a crea fișierul .jar
# Folosim -x test pentru a sări peste teste și a accelera procesul
RUN gradle build -x test --no-daemon

# --- Faza 2: Rularea aplicației ---
# Folosim o imagine mult mai mică, care conține doar Java Runtime
FROM eclipse-temurin:17-jre-jammy

# Expunem portul pe care va rula aplicația (Render folosește 10000 by default)
EXPOSE 10000

# Copiem doar fișierul .jar construit în faza anterioară
# Acest lucru face imaginea finală foarte mică și eficientă
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Specificăm comanda care va porni aplicația
ENTRYPOINT ["java","-jar","/app.jar"]
