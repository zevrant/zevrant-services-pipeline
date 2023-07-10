REVOKE ALL ON SCHEMA public FROM public;
create database $DATABASE_NAME;
\connect $DATABASE_NAME;
create role app_user NOCREATEROLE NOCREATEDB NOSUPERUSER;
create role change_management;
REVOKE ALL ON SCHEMA public FROM user_role;
REVOKE ALL ON SCHEMA public FROM change_management;
grant SELECT, UPDATE, INSERT to app_user;
grant update, insert to change_management;
grant create to change_management
create user $APP_USER with password $USER_PASSWORD;
create user liquibase with password $LIQUIBASE_PASSWORD;
grant app_user to $APP_USER;
grant change_management to liquibase;
