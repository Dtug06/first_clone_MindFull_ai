SELECT 'CREATE DATABASE mindbridge_dev'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mindbridge_dev')\gexec