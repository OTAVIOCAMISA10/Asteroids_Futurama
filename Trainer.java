import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Trainer {
    private static final int POPULATION_SIZE = 50;  // Número de redes por geração
    private static final int GENERATIONS = 100;     // Número de gerações
    private static final double MUTATION_RATE = 0.1; // Probabilidade de mutação
    private static final int ELITE_COUNT = 5;       // Top redes que sobrevivem sem mutação

    private List<RedeNeural> population;

    public Trainer() {
        initializePopulation();
    }

    private void initializePopulation() {
        population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(new RedeNeural(1, 8, 10, 3)); // Mesmo config da Fase
        }
    }

    // Avalia uma rede jogando o jogo (simula ticks e calcula fitness)
    private double evaluateFitness(RedeNeural nn) {
        // Simulação simplificada: crie uma instância de Fase e rode por alguns ticks
        // Nota: Isso é um placeholder; em produção, integre com o loop de jogo real
        Fase fase = new Fase(nn); // Modifique Fase para aceitar uma NN externa
        int ticks = 0;
        int maxTicks = 1000; // Limite para evitar loops infinitos
        while (fase.getPlayer().isVisivel() && ticks < maxTicks) {
            fase.actionPerformed(null); // Simula um tick
            ticks++;
        }
        // Fitness: kills + ticks sobrevividos
        return fase.getKills() + ticks * 0.1; // Peso para sobrevivência
    }

    public void train() {
        for (int gen = 0; gen < GENERATIONS; gen++) {
            // Avalia fitness de cada rede
            List<double[]> fitnesses = new ArrayList<>();
            for (RedeNeural nn : population) {
                double fit = evaluateFitness(nn);
                fitnesses.add(new double[]{fit, population.indexOf(nn)});
            }

            // Ordena por fitness (decrescente)
            fitnesses.sort(Comparator.comparingDouble(a -> -a[0]));

            // Seleciona elite
            List<RedeNeural> newPopulation = new ArrayList<>();
            for (int i = 0; i < ELITE_COUNT; i++) {
                int idx = (int) fitnesses.get(i)[1];
                newPopulation.add(population.get(idx));
            }

            // Crossover e mutação para preencher o resto
            while (newPopulation.size() < POPULATION_SIZE) {
                int idx1 = (int) fitnesses.get((int) (Math.random() * ELITE_COUNT))[1];
                int idx2 = (int) fitnesses.get((int) (Math.random() * ELITE_COUNT))[1];
                RedeNeural child = population.get(idx1).crossover(population.get(idx2));
                child.mutate(MUTATION_RATE);
                newPopulation.add(child);
            }

            population = newPopulation;
            System.out.println("Geração " + gen + " - Melhor Fitness: " + fitnesses.get(0)[0]);
        }
    }

    public RedeNeural getBestNetwork() {
        return population.get(0); // Retorna a melhor rede treinada
    }
}