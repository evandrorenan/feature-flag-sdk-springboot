-- Create ENUM types for PostgreSQL
CREATE TYPE flag_type AS ENUM ('BOOLEAN', 'STRING', 'NUMBER', 'OBJECT');
CREATE TYPE state AS ENUM ('ENABLED', 'DISABLED');

CREATE TABLE public.flags (
	id serial4 NOT NULL,
	flag_name varchar(255) NOT NULL,
	flag_type public.flag_type NOT NULL,
	state public.state NOT NULL,
	default_variant varchar(255) NOT NULL,
	targeting jsonb NULL,
	CONSTRAINT flags_flag_name_key UNIQUE (flag_name),
	CONSTRAINT flags_pkey PRIMARY KEY (id)
);

CREATE TABLE public.variants (
	flag_id int4 NOT NULL,
	variant_key varchar(255) NOT NULL,
	variant_value varchar(255) NOT NULL,
	CONSTRAINT variants_pkey PRIMARY KEY (flag_id, variant_key),
	CONSTRAINT variants_flag_id_fkey FOREIGN KEY (flag_id) REFERENCES public.flags(id) ON DELETE CASCADE
);