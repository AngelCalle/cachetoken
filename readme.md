# Ver definición completa del pod (env, mounts, args…)
kubectl get pod <pod-name> -n <ns> -o yaml

# Variables de entorno dentro del contenedor
kubectl exec -n <ns> -it <pod-name> -- printenv | sort

# Si ves envFrom o env: en el YAML, inspecciona esos objetos:
kubectl get configmap -n <ns> <cm-name> -o yaml
kubectl get secret -n <ns> <secret-name> -o yaml  # ¡ojo con credenciales!



# Ver qué hay en /config y otras rutas comunes
kubectl exec -n <ns> <pod-name> -- sh -c 'ls -la /config || true'
kubectl exec -n <ns> <pod-name> -- sh -c 'ls -la /opt/app/config || true'
kubectl exec -n <ns> <pod-name> -- sh -c 'ls -la /app || true'

# Leer archivos si existen
kubectl exec -n <ns> <pod-name> -- sh -c 'for f in /config/*; do echo "=== $f ==="; cat "$f"; echo; done'



# Localiza el jar
kubectl exec -n <ns> <pod-name> -- sh -c 'ls -la /app/*.jar || ls -la *.jar'

# Inspecciona el contenido sin extraer todo
kubectl exec -n <ns> <pod-name> -- sh -c 'jar tf /app/app.jar | grep -E "application.*(ya?ml|properties)$"'

# Muestra los archivos encontrados
kubectl exec -n <ns> <pod-name> -- sh -c 'jar xf /app/app.jar BOOT-INF/classes/application.properties; cat BOOT-INF/classes/application.properties || true'
kubectl exec -n <ns> <pod-name> -- sh -c 'for f in BOOT-INF/classes/application*.*; do echo "=== $f ==="; cat "$f"; echo; done || true'
