-- Declare une entreprise dans le registre, sur la base principale. Voir documentation/multitenancy.md.
--
-- Entreprise sur la base principale :
--   psql "$DATABASE_URL" -v id=acme -f documentation/scripts/ajouter-tenant.sql
--
-- Entreprise sur sa propre base (la variable nommee par secret_ref doit exister dans l'environnement
-- de l'application avant le redemarrage) :
--   psql "$DATABASE_URL" -v id=acme \
--     -v jdbc_url=jdbc:postgresql://pg-acme:5432/acme -v username=acme \
--     -v secret_ref=GLM_TENANT_ACME_DB_PASSWORD -v pool_max_size=10 \
--     -f documentation/scripts/ajouter-tenant.sql
--
-- L'entreprise n'est servie qu'apres redemarrage de l'application, qui cree et migre son schema.

\set ON_ERROR_STOP on
\if :{?schema_name} \else \set schema_name :id \endif
\if :{?jdbc_url} \else \set jdbc_url '' \endif
\if :{?username} \else \set username '' \endif
\if :{?secret_ref} \else \set secret_ref '' \endif
\if :{?pool_max_size} \else \set pool_max_size '' \endif

INSERT INTO public.tenant (id, schema_name, jdbc_url, username, secret_ref, pool_max_size, status)
VALUES (
  :'id',
  :'schema_name',
  NULLIF(:'jdbc_url', ''),
  NULLIF(:'username', ''),
  NULLIF(:'secret_ref', ''),
  NULLIF(:'pool_max_size', '')::int,
  'ACTIVE'
);
