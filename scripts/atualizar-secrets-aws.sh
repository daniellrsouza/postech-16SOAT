#!/usr/bin/env bash
# Atualiza AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY e AWS_SESSION_TOKEN nos 4
# repositórios da Fase 3 a partir de um profile de ~/.aws/credentials.
#
# As credenciais do AWS Academy (Learner Lab) expiram a cada sessão: cole o
# bloco "AWS CLI" do lab em ~/.aws/credentials e rode este script.
#
# Uso: ./scripts/atualizar-secrets-aws.sh [profile]   (default: "default")
# Requer: gh autenticado com acesso de admin aos repositórios; aws cli.
set -euo pipefail

PROFILE="${1:-default}"
OWNER="daniellrsouza"
REPOS=(postech-16soat-app postech-16soat-infra-k8s postech-16soat-infra-db postech-16soat-lambda)
CREDENTIALS_FILE="${AWS_SHARED_CREDENTIALS_FILE:-$HOME/.aws/credentials}"

ler_chave() {
  awk -v profile="[$PROFILE]" -v key="$1" '
    $0 == profile { in_profile = 1; next }
    /^\[/          { in_profile = 0 }
    in_profile {
      split($0, kv, "=")
      gsub(/[ \t\r]/, "", kv[1])
      if (kv[1] == key) {
        value = substr($0, index($0, "=") + 1)
        gsub(/^[ \t]+|[ \t\r]+$/, "", value)
        print value
        exit
      }
    }
  ' "$CREDENTIALS_FILE"
}

[ -f "$CREDENTIALS_FILE" ] || { echo "Arquivo não encontrado: $CREDENTIALS_FILE" >&2; exit 1; }

declare -A SECRETS=(
  [AWS_ACCESS_KEY_ID]="$(ler_chave aws_access_key_id)"
  [AWS_SECRET_ACCESS_KEY]="$(ler_chave aws_secret_access_key)"
  [AWS_SESSION_TOKEN]="$(ler_chave aws_session_token)"
)

for nome in "${!SECRETS[@]}"; do
  [ -n "${SECRETS[$nome]}" ] || { echo "Chave ausente no profile [$PROFILE]: $nome" >&2; exit 1; }
done

echo "Validando as credenciais do profile [$PROFILE]..."
aws sts get-caller-identity --profile "$PROFILE" --query Arn --output text

for repo in "${REPOS[@]}"; do
  for nome in "${!SECRETS[@]}"; do
    # Valor via stdin, para não aparecer na lista de processos.
    printf '%s' "${SECRETS[$nome]}" | gh secret set "$nome" -R "$OWNER/$repo"
  done
  echo "Secrets AWS atualizados em $OWNER/$repo"
done

# --- Saúde do cluster EKS ----------------------------------------------------
# Quando a sessão do lab encerra, o AWS Academy bloqueia inclusive a LabRole dos
# nós. Se o node group trocar instâncias nesse intervalo, as novas sobem mas o
# nodeadm falha em registrá-las no EKS e não tenta de novo. Um reboot resolve,
# porque o nodeadm roda a cada boot.
#
# SO_VERIFICAR=1 só relata o que seria feito, sem reiniciar nada.
CLUSTER="eks-tech-challenge"
NODEGROUP="nodeg-tech-challenge"
REGION="${AWS_REGION:-us-east-1}"

if ! aws eks describe-cluster --profile "$PROFILE" --region "$REGION" --name "$CLUSTER" >/dev/null 2>&1; then
  echo "Cluster $CLUSTER não existe nesta conta; verificação dos nós ignorada."
  exit 0
fi

aws eks update-kubeconfig --profile "$PROFILE" --region "$REGION" --name "$CLUSTER" >/dev/null

# No Windows, aws.exe e kubectl.exe terminam as linhas com CRLF: tr remove o CR.
INSTANCIAS="$(aws ec2 describe-instances --profile "$PROFILE" --region "$REGION" \
  --filters "Name=tag:eks:nodegroup-name,Values=$NODEGROUP" "Name=instance-state-name,Values=running" \
  --query "Reservations[].Instances[].InstanceId" --output text | tr -d '\r' | tr '\t' '\n')"

# providerID do nó = aws:///<az>/<instance-id>; lista só os nós Ready.
PRONTAS="$(kubectl get nodes -o jsonpath='{range .items[*]}{.spec.providerID}{" "}{.status.conditions[?(@.type=="Ready")].status}{"\n"}{end}' 2>/dev/null \
  | tr -d '\r' | awk '$2 == "True" {n = split($1, p, "/"); print p[n]}')"

PARA_REINICIAR=()
TOTAL=0
for id in $INSTANCIAS; do
  TOTAL=$((TOTAL + 1))
  grep -qx "$id" <<<"$PRONTAS" || PARA_REINICIAR+=("$id")
done

if [ ${#PARA_REINICIAR[@]} -eq 0 ]; then
  echo "Cluster $CLUSTER: $TOTAL nó(s) Ready."
  exit 0
fi

echo "Nós sem registro Ready no cluster: ${PARA_REINICIAR[*]}"
if [ "${SO_VERIFICAR:-0}" = "1" ]; then
  echo "SO_VERIFICAR=1: nenhum reboot executado."
  exit 0
fi

echo "Reiniciando..."
aws ec2 reboot-instances --profile "$PROFILE" --region "$REGION" --instance-ids "${PARA_REINICIAR[@]}"
for _ in $(seq 1 30); do
  prontos=$(kubectl get nodes --no-headers 2>/dev/null | tr -d '\r' | awk '$2 == "Ready"' | wc -l)
  if [ "$prontos" -ge "$TOTAL" ]; then
    echo "Cluster $CLUSTER: $prontos nó(s) Ready após o reboot."
    exit 0
  fi
  sleep 10
done
echo "Atenção: os nós não ficaram Ready em 5 minutos. Veja o console da instância (nodeadm)." >&2
exit 1
