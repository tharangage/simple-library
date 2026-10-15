#!/usr/bin/env bash
# Deletes the whole local cluster, including the database volume.
set -euo pipefail
kind delete cluster --name simple-library
