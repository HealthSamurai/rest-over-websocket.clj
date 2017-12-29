# cd backend
# lein uberjar
# docker build -t niquola/rest-over-vs:latest .
# docker push niquola/rest-over-vs:latest

# cd ..

cd ui
rm -rf build
mkdir build
cp -r resources/public/* build
rm -rf build/js
lein with-profile prod cljsbuild once
docker build -t niquola/rest-over-vs-ui:latest .
docker push niquola/rest-over-vs-ui:latest

# cd ..
# cd db
# docker build -t niquola/pglogical:latest .
# docker push niquola/pglogical:latest
