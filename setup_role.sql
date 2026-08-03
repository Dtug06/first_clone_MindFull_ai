DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname='mindbridge_app') THEN
    CREATE ROLE mindbridge_app LOGIN PASSWORD 'mindbridge_app_dev';
  END IF;
END
$$;