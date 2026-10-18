"""crear_tablas_faltantes: crea solo lo que falta y es idempotente."""

from unittest.mock import MagicMock

from app.common.dynamodb.creador_tablas import crear_tablas_faltantes
from app.common.dynamodb.tablas import DEFINICION_TABLAS, TABLA_SESIONES


def _cliente_con_tablas(existentes: list[str]) -> MagicMock:
    cliente = MagicMock()
    cliente.list_tables.return_value = {"TableNames": existentes}
    return cliente


def test_sin_tablas_crea_todas_las_definidas() -> None:
    cliente = _cliente_con_tablas([])

    crear_tablas_faltantes(cliente)

    creadas = [c.kwargs["TableName"] for c in cliente.create_table.call_args_list]
    assert creadas == [d.tabla["TableName"] for d in DEFINICION_TABLAS]


def test_no_vuelve_a_crear_una_tabla_que_ya_existe() -> None:
    cliente = _cliente_con_tablas([TABLA_SESIONES])

    crear_tablas_faltantes(cliente)

    creadas = [c.kwargs["TableName"] for c in cliente.create_table.call_args_list]
    assert TABLA_SESIONES not in creadas
    assert len(creadas) == len(DEFINICION_TABLAS) - 1


def test_con_todas_las_tablas_existentes_no_crea_nada() -> None:
    cliente = _cliente_con_tablas([d.tabla["TableName"] for d in DEFINICION_TABLAS])

    crear_tablas_faltantes(cliente)

    cliente.create_table.assert_not_called()
