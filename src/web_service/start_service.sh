svshi-web-service run -a $(hostname -i):4545 --docker &
cd ${SVSHI_HOME}/src/web_service/frontend/svshi-web-service-frontend 
serve -s dist