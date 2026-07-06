#!/bin/sh
set -e

mkdir -p /data/books /data/chess
chown -R chessreader:chessreader /data

exec gosu chessreader java -jar app.jar
