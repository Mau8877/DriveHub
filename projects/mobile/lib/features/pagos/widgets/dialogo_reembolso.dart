import 'package:flutter/material.dart';

import '../models/motivo_reembolso.dart';
import '../models/pago_detalle.dart';
import 'formato_dinero.dart';

/// Lo que la persona eligió en el diálogo del reembolso: el motivo y, solo con «Otro», lo que escribió.
typedef MotivoElegido = ({MotivoReembolso motivo, String? detalle});

/// Pide confirmar el reembolso de una compra y su motivo (CU31). Devuelve el motivo elegido, o `null` si la
/// persona cancela.
///
/// * Muestra lo que recibirá, lo que se retiene y las horas que se descontarán (todo viene del backend).
/// * «Solicitar» está deshabilitado hasta que haya un motivo, y con «Otro», hasta que el texto tenga de 5 a 200
///   caracteres.
Future<MotivoElegido?> pedirMotivoDeReembolso(
  BuildContext context,
  PagoDetalle pago,
) {
  return showDialog<MotivoElegido>(
    context: context,
    builder: (_) => _DialogoReembolso(pago: pago),
  );
}

class _DialogoReembolso extends StatefulWidget {
  final PagoDetalle pago;

  const _DialogoReembolso({required this.pago});

  @override
  State<_DialogoReembolso> createState() => _DialogoReembolsoState();
}

class _DialogoReembolsoState extends State<_DialogoReembolso> {
  final _detalle = TextEditingController();
  MotivoReembolso? _motivo;

  @override
  void dispose() {
    _detalle.dispose();
    super.dispose();
  }

  bool get _completo {
    final motivo = _motivo;
    if (motivo == null) return false;
    return motivo != MotivoReembolso.otro ||
        MotivoReembolso.detalleValido(_detalle.text);
  }

  void _solicitar() {
    final motivo = _motivo!;
    Navigator.of(context).pop((
      motivo: motivo,
      detalle: motivo == MotivoReembolso.otro ? _detalle.text.trim() : null,
    ));
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final reembolso = widget.pago.reembolso;

    return AlertDialog(
      title: const Text('Solicitar reembolso'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (reembolso != null)
              Text(
                'Recibirás ${formatearUsd(reembolso.montoDevuelto)}; se retiene '
                '${formatearUsd(reembolso.montoRetenido)} '
                '(${reembolso.porcentajeRetenido} %).',
                key: const Key('textoMontosReembolso'),
              ),
            const SizedBox(height: 8),
            Text(
              'Se descontarán ${widget.pago.horas} horas de tu saldo.',
              key: const Key('textoHorasReembolso'),
            ),
            const SizedBox(height: 16),
            Text('¿Por qué lo pides?', style: tema.textTheme.titleSmall),
            RadioGroup<MotivoReembolso>(
              groupValue: _motivo,
              onChanged: (valor) => setState(() => _motivo = valor),
              child: Column(
                children: [
                  for (final motivo in MotivoReembolso.values)
                    RadioListTile<MotivoReembolso>(
                      key: Key('motivo_${motivo.valorApi}'),
                      value: motivo,
                      contentPadding: EdgeInsets.zero,
                      title: Text(motivo.texto),
                    ),
                ],
              ),
            ),
            if (_motivo == MotivoReembolso.otro)
              TextField(
                key: const Key('campoMotivoDetalle'),
                controller: _detalle,
                maxLength: MotivoReembolso.detalleMaximo,
                maxLines: 3,
                minLines: 2,
                textCapitalization: TextCapitalization.sentences,
                onChanged: (_) => setState(() {}),
                decoration: const InputDecoration(
                  labelText: 'Cuéntanos el motivo',
                  helperText: 'De 5 a 200 caracteres',
                ),
              ),
          ],
        ),
      ),
      actions: [
        TextButton(
          key: const Key('botonCancelarReembolso'),
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          key: const Key('botonConfirmarReembolso'),
          onPressed: _completo ? _solicitar : null,
          child: const Text('Solicitar'),
        ),
      ],
    );
  }
}
