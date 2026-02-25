FROM quay.io/wildfly/wildfly:31.0.0.Final-jdk17

# Copier le WAR dans le répertoire de déploiement de WildFly
COPY target/products-api.war /opt/jboss/wildfly/standalone/deployments/

# Exposer le port 8080
EXPOSE 8080

# Démarrer WildFly en mode standalone
# -b 0.0.0.0 : bind sur toutes les interfaces réseau (nécessaire pour Docker)
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
