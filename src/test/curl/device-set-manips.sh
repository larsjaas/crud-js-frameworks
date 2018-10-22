#!/bin/bash -x

urlbase=http://localhost:8080

curl $urlbase/device
echo ""

curl $urlbase/device/new
echo ""

curl $urlbase/device
echo ""

newid=`uuid`
curl -X POST -H "Content-Type: application/json" -d "{\"id\":\"$newid\",\"key\":1}" $urlbase/device/$newid
echo ""

curl $urlbase/device
echo ""

curl -X DELETE $urlbase/device/$newid
echo ""

curl $urlbase/device
echo ""

id2=`uuid`
curl -X POST -H "Content-Type: application/json" -d "{\"id\":\"$id2\",\"key\":3}" $urlbase/device/$id2
echo ""
curl -X PATCH -H "Content-Type: application/json" -d "{\"id\":\"$id2\",\"kjeks\":45}" $urlbase/device/$id2
echo ""
curl -X PATCH -H "Content-Type: application/json" -d "{\"id\":\"$id2\",\"kjeks\":123}" $urlbase/device/$id2
echo ""

curl $urlbase/device
echo ""
