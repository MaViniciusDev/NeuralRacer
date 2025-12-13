package org.MaViniciusDev.ia;

import java.util.Random;

public class CerebroGenetico {
    // AUMENTAMOS PARA 20 REGRAS:
    // Regras 0-9: Controlam Direção
    // Regras 10-19: Controlam Aceleração
    private static final int NUM_REGRAS = 20;

    // Cada regra: 1 bit (Ativa/Inativa) + 125 bits (OU) + 125 bits (E) = 251 bits
    private static final int BITS_POR_REGRA = 251;
    private static final int TAMANHO_GENOMA = NUM_REGRAS * BITS_POR_REGRA;

    private boolean[] cromossomo;
    private final Random random = new Random();

    public CerebroGenetico() {
        this.cromossomo = new boolean[TAMANHO_GENOMA];
        randomizar();
    }

    public CerebroGenetico(boolean[] genes) {
        this.cromossomo = genes;
    }

    public void randomizar() {
        // Inicialização esparsa (2% de chance) para evitar bloqueio lógico
        for (int i = 0; i < TAMANHO_GENOMA; i++) {
            cromossomo[i] = random.nextDouble() < 0.02;
        }
        // Ativa aleatoriamente as regras
        for (int r = 0; r < NUM_REGRAS; r++) {
            int offset = r * BITS_POR_REGRA;
            cromossomo[offset] = random.nextBoolean();
        }
    }

    // --- MÉTODOS DE DECISÃO ---

    /**
     * Usa as regras 0 a 9 para decidir a direção (-45 a +45 graus)
     */
    public double processarDirecao(double[] inputsFuzzy) {
        // Mapeamento de saída para direção: Esquerda (-45) a Direita (+45)
        double[] valoresSaida = {-45, -22.5, 0, 22.5, 45};
        // Processa apenas as primeiras 10 regras (offset 0 a 9)
        return processarFuzzyGenerico(inputsFuzzy, valoresSaida, 0, 10);
    }

    /**
     * Usa as regras 10 a 19 para decidir a aceleração (-1.0 a +1.0)
     */
    public double processarAceleracao(double[] inputsFuzzy) {
        // Mapeamento de saída para pedal:
        // Nível 0 (Muito Baixo) -> Freio Total (-1.0)
        // Nível 1 (Baixo)       -> Freio Leve (-0.5)
        // Nível 2 (Médio)       -> Ponto Morto (0.0)
        // Nível 3 (Alto)        -> Aceleração Média (0.5)
        // Nível 4 (Muito Alto)  -> Aceleração Total (1.0)
        double[] valoresSaida = {-1.0, -0.5, 0.0, 0.5, 1.0};

        // Processa as regras 10 a 19
        return processarFuzzyGenerico(inputsFuzzy, valoresSaida, 10, 20);
    }

    /**
     * Método genérico que processa um intervalo específico de regras
     */
    private double processarFuzzyGenerico(double[] inputs, double[] valoresConsequentes, int regraInicio, int regraFim) {
        double[] saidas = new double[5]; // Acumuladores para os 5 níveis de saída

        for (int r = regraInicio; r < regraFim; r++) {
            int offset = r * BITS_POR_REGRA;

            if (!cromossomo[offset]) continue; // Regra inativa

            for (int saidaIdx = 0; saidaIdx < 5; saidaIdx++) {
                double ativacaoOU = 0.0;
                double ativacaoE = 1.0;
                boolean usouBitOU = false;
                boolean usouBitE = false;

                for (int entradaIdx = 0; entradaIdx < 25; entradaIdx++) {
                    int bitIndex = (entradaIdx * 5) + saidaIdx;

                    // OU
                    if (cromossomo[offset + 1 + bitIndex]) {
                        ativacaoOU = Math.max(ativacaoOU, inputs[entradaIdx]);
                        usouBitOU = true;
                    }
                    // E
                    if (cromossomo[offset + 1 + 125 + bitIndex]) {
                        ativacaoE = Math.min(ativacaoE, inputs[entradaIdx]);
                        usouBitE = true;
                    }
                }

                double resultadoRegra = 0.0;
                if (usouBitOU && usouBitE) resultadoRegra = Math.min(ativacaoOU, ativacaoE);
                else if (usouBitOU) resultadoRegra = ativacaoOU;
                else if (usouBitE) resultadoRegra = ativacaoE;

                saidas[saidaIdx] = Math.max(saidas[saidaIdx], resultadoRegra);
            }
        }

        // Defuzzificação (Centro de Gravidade)
        double numerador = 0;
        double denominador = 0;

        for (int i = 0; i < 5; i++) {
            numerador += saidas[i] * valoresConsequentes[i];
            denominador += saidas[i];
        }

        if (denominador == 0) return 0.0; // Padrão se nenhuma regra ativar

        // Retorna o valor ponderado
        // Não precisamos de clamp aqui se os valoresConsequentes já estiverem nos limites
        return numerador / denominador;
    }

    public boolean[] getCromossomo() {
        return cromossomo;
    }

    public void mutar(double taxa) {
        for (int i = 0; i < TAMANHO_GENOMA; i++) {
            if (random.nextDouble() < taxa) {
                cromossomo[i] = !cromossomo[i];
            }
        }
    }
}