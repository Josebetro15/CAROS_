-- ScanOT · Spring Boot · pgAdmin 4
-- 1. Create Database → nombre: scanot
-- 2. Query Tool sobre scanot → pega esto → F5

begin;

create table if not exists usuarios (
  id bigserial primary key,
  email text not null unique,
  password text not null,
  display_name text not null,
  rol text not null
);

create table if not exists vehicles (
  id serial primary key,
  plate text not null unique,
  brand text not null,
  model text not null,
  year integer not null,
  km integer not null default 0,
  vin text not null unique,
  health integer not null default 100,
  owner_user_id bigint references usuarios(id),
  created_at timestamptz not null default now()
);

create table if not exists parts (
  id serial primary key,
  sku text not null unique,
  name text not null,
  category text not null,
  price numeric(10,2) not null,
  stock integer not null default 0,
  compatibility text not null default ''
);

create table if not exists signals (
  id serial primary key,
  vehicle_id integer not null references vehicles(id),
  code text not null,
  description text not null,
  severity text not null,
  suggestion text not null,
  status text not null default 'open',
  created_at timestamptz not null default now(),
  closed_at timestamptz
);

create table if not exists work_orders (
  id serial primary key,
  folio text not null unique,
  vehicle_id integer not null references vehicles(id),
  work_desc text not null,
  technician text not null,
  scheduled_at timestamptz not null default now(),
  hours numeric(5,1) not null default 1,
  status text not null default 'pending',
  created_at timestamptz not null default now()
);

create table if not exists scan_readings (
  id serial primary key,
  vehicle_id integer not null references vehicles(id),
  vin text not null,
  km integer not null,
  source text not null,
  dtc_codes text not null default '',
  created_new boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists vehicle_updates (
  id serial primary key,
  vehicle_id integer not null references vehicles(id),
  author_user_id bigint not null references usuarios(id),
  kind text not null,
  message text not null,
  created_at timestamptz not null default now()
);

create table if not exists update_media (
  id serial primary key,
  update_id integer not null references vehicle_updates(id) on delete cascade,
  media_type text not null,
  url text not null
);

commit;

-- Las cuentas (cliente / mecanico / encargado) y los autos de demo
-- las crea Spring al arrancar (password: scanot123)
