# Run in workspace after: tools vault write-file --key password --name "GitHub apps token" --return-channel whatsapp  (in tools box)
set -e; cd /tmp/st/repo; P=/home/sandbox/.vault/github-apps-token-06dc2e/password
AUTH="Authorization: Basic $(printf 'x-access-token:%s' "$(cat $P)" | base64 -w0)"
git add -A; git commit -q -m "${1:-Publish}" || true; git -c http.extraheader="$AUTH" pull -q --rebase origin main || true; git -c http.extraheader="$AUTH" push -q origin HEAD:main; rm -f $P; git log --oneline | head -2
