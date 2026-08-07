package de.TeutonStudio.MathematikRechenSystem.kern

private const val TENSOR_FORMEL_ACHSEN_TUPEL = "__tensor.achsen.tupel"
private const val TENSOR_FORMEL_ACHSE_PREFIX = "__tensor.achse."
private const val TENSOR_FORMEL_PARAMETER_PREFIX = "__tensor.parameter."

/**
 * Verlustfreier Adapter zwischen einer Tensoroperation und der gemeinsamen
 * Formel-DAG. Rollen, Achsen und Parameter bleiben echte Formelargumente.
 */
object TensorOperationFormelCodec {
    fun zuFormel(operation: TensorOperation): FormelAusdruck.Operation {
        var position = 0
        val argumente = buildList {
            operation.operanden.entries.sortedBy { it.key.wert }.forEach { (rolle, objekt) ->
                add(
                    FormelArgument(
                        rollenId = rolle.wert,
                        position = position++,
                        ausdruck = objekt.alsTensorFormelLiteral("operand-${rolle.wert}"),
                    ),
                )
            }
            when (val achsen = operation.achsen) {
                null -> Unit
                is TensorAchsenSpezifikation.Tupel -> add(
                    FormelArgument(
                        rollenId = TENSOR_FORMEL_ACHSEN_TUPEL,
                        position = position++,
                        ausdruck = Tupel(
                            achsen.sichtbareIndizes.map { RationaleZahl.von(it.toLong()) },
                        ).alsTensorFormelLiteral("achsen"),
                    ),
                )
                is TensorAchsenSpezifikation.Dynamisch -> achsen.sichtbareIndizesNachRolle.entries
                    .sortedBy { it.key.wert }
                    .forEach { (rolle, wert) ->
                        add(
                            FormelArgument(
                                rollenId = "$TENSOR_FORMEL_ACHSE_PREFIX${rolle.wert}",
                                position = position++,
                                ausdruck = RationaleZahl.von(wert.toLong())
                                    .alsTensorFormelLiteral("achse-${rolle.wert}"),
                            ),
                        )
                    }
            }
            operation.parameter.entries.sortedBy { it.key }.forEach { (id, wert) ->
                add(
                    FormelArgument(
                        rollenId = "$TENSOR_FORMEL_PARAMETER_PREFIX$id",
                        position = position++,
                        ausdruck = wert.alsTensorFormelLiteral("parameter-$id"),
                    ),
                )
            }
        }
        return FormelAusdruck.Operation(
            id = "tensor-operation-${operation.operationId.wert}",
            operatorId = operation.operationId.wert,
            argumente = argumente,
            typ = FormelTyp.OBJEKT,
            bedingungen = operation.voraussetzungen.toList(),
        )
    }

    fun ausFormel(formel: FormelAusdruck.Operation): TensorOperation {
        val definition = requireNotNull(StandardTensorOperationen.registry.definition(formel.operatorId)) {
            "Die Formeloperation '${formel.operatorId}' ist keine registrierte Tensoroperation."
        }
        val argumenteNachRolle = formel.argumente.associateBy { it.rollenId }
        val operanden = definition.eingangsRollen.associateWith { rolle ->
            val argument = requireNotNull(argumenteNachRolle[rolle.wert]) {
                "Der Tensorformel fehlt die Operandenrolle '${rolle.wert}'."
            }
            argument.ausdruck.alsMathematischesTensorObjekt()
        }
        val achsen = argumenteNachRolle[TENSOR_FORMEL_ACHSEN_TUPEL]?.let { argument ->
            val tupel = argument.ausdruck.alsMathematischesTensorObjekt() as? Tupel
                ?: error("Das Achsenargument muss ein Tupel sein.")
            TensorAchsenSpezifikation.Tupel(
                tupel.elemente.map { element ->
                    val zahl = element as? RationaleZahl
                        ?: error("Tensorachsen müssen ganze Zahlen sein.")
                    require(zahl.nenner == java.math.BigInteger.ONE)
                    zahl.zähler.intValueExact()
                },
            )
        } ?: formel.argumente
            .filter { it.rollenId.startsWith(TENSOR_FORMEL_ACHSE_PREFIX) }
            .sortedBy { it.position }
            .takeIf { it.isNotEmpty() }
            ?.associate { argument ->
                val rollenId = argument.rollenId.removePrefix(TENSOR_FORMEL_ACHSE_PREFIX)
                val zahl = argument.ausdruck.alsMathematischesTensorObjekt() as? RationaleZahl
                    ?: error("Ein dynamisches Achsenargument muss eine ganze Zahl sein.")
                require(zahl.nenner == java.math.BigInteger.ONE)
                TensorHandleRolle(rollenId) to zahl.zähler.intValueExact()
            }
            ?.let(TensorAchsenSpezifikation::Dynamisch)
        val parameter = formel.argumente
            .filter { it.rollenId.startsWith(TENSOR_FORMEL_PARAMETER_PREFIX) }
            .associate { argument ->
                argument.rollenId.removePrefix(TENSOR_FORMEL_PARAMETER_PREFIX) to
                    argument.ausdruck.alsMathematischesTensorObjekt()
            }
        return TensorOperation(
            operationId = definition.id,
            operanden = operanden,
            achsen = achsen,
            parameter = parameter,
            voraussetzungen = formel.bedingungen.toSet(),
            unterstuetzungsStatus = definition.unterstuetzungsStatus,
        )
    }
}

private fun MathematischesObjekt.alsTensorFormelLiteral(suffix: String): FormelAusdruck.Literal =
    FormelAusdruck.Literal(
        id = "tensor-literal-$suffix",
        wert = this,
        typ = when (this) {
            is ZahlAusdruck -> FormelTyp.ZAHL
            is Tupel -> FormelTyp.TUPEL
            is Tensor -> FormelTyp.TENSOR
            is Matrix -> FormelTyp.MATRIX
            is ZeilenVektor, is SpaltenVektor -> FormelTyp.VEKTOR
            else -> FormelTyp.OBJEKT
        },
    )

private fun FormelAusdruck.alsMathematischesTensorObjekt(): MathematischesObjekt = when (this) {
    is FormelAusdruck.Literal -> wert
    is FormelAusdruck.Variable -> Variable(name, latex)
    is FormelAusdruck.Platzhalter -> AllgemeinerParameter(id, FormelRenderer.render(this).latex)
    is FormelAusdruck.Operation -> AllgemeinerParameter(id, FormelRenderer.render(this).latex)
}
