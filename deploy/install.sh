#!/usr/bin/env bash
# ============================================================
#  mall-oymy 一键上线（腾讯云 + Cloudflare Tunnel）
#  用法：sudo bash deploy/install.sh
#  幂等，可以重复执行。
# ============================================================
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$DEPLOY_DIR")"
cd "$DEPLOY_DIR"

c() { printf '\033[36m%s\033[0m\n' "$*"; }
ok() { printf '\033[32m%s\033[0m\n' "$*"; }
die() { printf '\033[31m[X] %s\033[0m\n' "$*" >&2; exit 1; }

[ "$(id -u)" = "0" ] || die "请用 root 运行：sudo bash deploy/install.sh"

# ---------- 1. 检查内存，决定是否启用搜索 ----------
MEM_MB=$(awk '/MemTotal/{print int($2/1024)}' /proc/meminfo)
c "[1/7] 服务器内存 ${MEM_MB} MB"
if [ "$MEM_MB" -lt 7500 ]; then
  USE_SEARCH=0
  c "      内存偏小，跳过 Elasticsearch / search-service（搜索接口会不可用）"
else
  USE_SEARCH=1
  c "      内存充足，启用 Elasticsearch"
fi

# ---------- 2. 安装 Docker ----------
c "[2/7] 检查 Docker"
if ! command -v docker >/dev/null 2>&1; then
  c "      安装 Docker ..."
  curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
  systemctl enable --now docker
fi
if docker compose version >/dev/null 2>&1; then
  DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  die "没找到 docker compose 插件"
fi
ok "      $($DC version --short 2>/dev/null || echo ok)"

# ---------- 3. 镜像加速 ----------
c "[3/7] 配置国内镜像加速"
if [ ! -f /etc/docker/daemon.json ]; then
  cat > /etc/docker/daemon.json <<'JSON'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.m.daocloud.io"
  ],
  "log-driver": "json-file",
  "log-opts": { "max-size": "50m", "max-file": "3" }
}
JSON
  systemctl restart docker
  ok "      已写入 /etc/docker/daemon.json 并重启 Docker"
else
  ok "      已存在，跳过"
fi

# ---------- 4. ES 需要的内核参数 ----------
c "[4/7] 内核参数 vm.max_map_count=262144"
sysctl -w vm.max_map_count=262144 >/dev/null
grep -q '^vm.max_map_count' /etc/sysctl.conf 2>/dev/null || echo 'vm.max_map_count=262144' >> /etc/sysctl.conf
ok "      ok"

# ---------- 5. 环境变量 ----------
c "[5/7] 检查 deploy/.env"
if [ ! -f .env ]; then
  cp .env.example .env
  rnd() { openssl rand -hex 16; }   # 32 位纯字母数字，不会有管道/SIGPIPE 问题
  sed -i "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=$(rnd)|" .env
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -hex 48)|" .env
  sed -i "s|^RABBITMQ_PASSWORD=.*|RABBITMQ_PASSWORD=$(rnd)|" .env
  ok "      已生成随机密码"
  c "      提示：Alipay 私钥这里没带过来，要用支付功能就从本地执行"
  c "            scp deploy/.env root@<你的IP>:$DEPLOY_DIR/.env"
fi
chmod 600 .env

if ! grep -qE '^TUNNEL_TOKEN=.+' .env; then
  echo
  die "还没填 TUNNEL_TOKEN。请：
    1) 打开 Cloudflare 面板 -> Zero Trust -> Networks -> Tunnels -> Create a tunnel
    2) 选 Cloudflared，起名 mall，复制命令里 --token 后面那一串
    3) 编辑 $DEPLOY_DIR/.env，让 TUNNEL_TOKEN=<那一串>
    4) 重新执行 sudo bash $DEPLOY_DIR/install.sh
    5) 回到 Tunnel 的 Public Hostname 页签加三条记录，service 都填 http://nginx:80
       .oymy-mall.online / www.oymy-mall.online / admin.oymy-mall.online"
fi
ok "      TUNNEL_TOKEN 已配置"

# ---------- 6. 构建 jar ----------
c "[6/7] 检查可执行 jar"
if ! ls "$REPO_DIR"/mall-gateway/target/mall-gateway-*-SNAPSHOT.jar >/dev/null 2>&1; then
  if [ "$MEM_MB" -lt 7000 ]; then
    die "内存不足 7G，不建议在服务器上跑 Maven。
    请在本地执行 mvn package -DskipTests，然后把 mall-*/target/*.jar 上传到服务器对应目录。"
  fi
  command -v mvn >/dev/null 2>&1 || die "没装 Maven（需要 JDK 23 + Maven）"
  c "      开始构建（约 2-5 分钟）..."
  ( cd "$REPO_DIR" && mvn -q package -DskipTests )
fi
ok "      jar 就绪"

# ---------- 7. 启动 ----------
c "[7/7] 启动容器"
if [ "$USE_SEARCH" = "1" ]; then
  $DC --profile search up -d --build
else
  $DC up -d --build
fi

c "      等待服务就绪（90 秒）..."
sleep 90
$DC ps
echo
curl -s -o /dev/null -w "      本地 nginx      -> HTTP %{http_code}\n" http://127.0.0.1:8088/ || true
curl -s -o /dev/null -w "      网关/C端接口    -> HTTP %{http_code}\n" http://127.0.0.1:8088/api/c/product/list || true
echo
ok "完成！"
c "接下来："
c "  1) Cloudflare 面板 -> 该 Tunnel -> Public Hostname 添加："
c "       oymy-mall.online        -> http://nginx:80"
c "       www.oymy-mall.online    -> http://nginx:80"
c "       admin.oymy-mall.online  -> http://nginx:80"
c "  2) 站点 SSL/TLS 设为 Full，并打开 Always Use HTTPS"
c "  3) 登录 https://admin.oymy-mall.online 后立刻改掉默认管理员 admin/123456"
c "  4) 安全组只保留 22 端口"
