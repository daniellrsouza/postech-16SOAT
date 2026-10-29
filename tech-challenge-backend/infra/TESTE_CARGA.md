# Exemplificação do autoscaling de pods do cluster

1. Irei rodar o comando: ```k -n tech-challenge get hpa tech-challenge-app -w``` (k é atalho para kubectl);
2. Acompanharei os targets (métricas) definidos para autoscaling;
3. Vou executar o teste de carga usando a imagem williamyeh/hey simulando inicialmente 150 requisições dentro de 5m para o endpoint de /ping. O comando usado foi: ```k -n tech-challenge run loadtest --rm -it --restart=Never \
  --image=williamyeh/hey -- \
  -z 5m -c 150 http://tech-challenge-app/ping```;
4. Caso não gere stress, irei aumentar o número de requisições.