-- CU08: la búsqueda de estudiantes por nombre no distingue tildes ("jose" encuentra "José").
CREATE EXTENSION IF NOT EXISTS unaccent;
