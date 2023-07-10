REVOKE ALL ON SCHEMA public FROM public;
create database $DATABASE_NAME;
\connect $DATABASE_NAME;
create role app_user NOCREATEROLE NOCREATEDB NOSUPERUSER;
create role liquibase;
REVOKE ALL ON SCHEMA public FROM user_role;
REVOKE ALL ON SCHEMA public FROM liquibase;
grant SELECT, UPDATE, INSERT to app_user;
grant update, insert to liquibase;
grant create table to liquibase
create user $APP_USER with password $USER_PASSWORD;
create user liquibase with password $LIQUIBASE_PASSWORD;
grant app_user to $APP_USER;
grant liquibase to liquibase;
