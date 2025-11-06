import java.util.Random;

public class RedeNeural {
    private static final Random RAND = new Random();

    // ---- Ativações ----
    private static double relu(double x) { return x > 0.0 ? x : 0.0; }
    private static double dreluFromOut(double y) { return y > 0.0 ? 1.0 : 0.0; } // como usamos out da ReLU
    private static double tanh(double x) { return Math.tanh(x); }
    private static double dtanhFromOut(double y) { return 1.0 - y*y; }
    private static double sigmoid(double x) { return 1.0 / (1.0 + Math.exp(-x)); }
    private static double dsigmoidFromOut(double y) { return y * (1.0 - y); }

    // 0=tanh, 1=sigmoid, 2=identity
    private static final int ACT_TANH = 0, ACT_SIGM = 1, ACT_ID = 2;

    private Layer inputLayer;
    private Layer[] hiddenLayers;
    private Layer outputLayer;

    private final int hiddenLayerCount;
    private final int inputNeuronCount;   // sem contar bias
    private final int hiddenNeuronCount;  // sem contar bias
    private final int outputNeuronCount;  // sem bias

    // tipo de ativação de cada neurônio de saída (dx,dy,fire)
    private final int[] outAct;

    public RedeNeural(int qtyHiddenLayers, int qtyInputs, int qtyHiddenNeurons, int qtyOutputs) {
        this.hiddenLayerCount  = qtyHiddenLayers;
        this.inputNeuronCount  = qtyInputs;
        this.hiddenNeuronCount = qtyHiddenNeurons;
        this.outputNeuronCount = qtyOutputs;

        int inputLayerSize  = qtyInputs + 1;         // +1 bias
        int hiddenLayerSize = qtyHiddenNeurons + 1;  // +1 bias

        this.inputLayer   = new Layer(inputLayerSize, 0, true);
        this.hiddenLayers = new Layer[qtyHiddenLayers];

        for (int i = 0; i < qtyHiddenLayers; i++) {
            int prevSize = (i == 0) ? inputLayerSize : hiddenLayerSize;
            this.hiddenLayers[i] = new Layer(hiddenLayerSize, prevSize, true);
        }

        this.outputLayer = new Layer(qtyOutputs, hiddenLayerSize, false);

        // dx, dy => tanh | fire => sigmoid | demais (se houver) -> identity
        this.outAct = new int[qtyOutputs];
        for (int i = 0; i < qtyOutputs; i++) {
            if (i == 0 || i == 1) outAct[i] = ACT_TANH;
            else if (i == 2)      outAct[i] = ACT_SIGM;
            else                  outAct[i] = ACT_ID;
        }
    }

    // ---- Forward ----
    public double[] feedForward(double[] inputs) {
        if (inputs == null || inputs.length != this.inputNeuronCount) {
            throw new IllegalArgumentException(
                "Tamanho de inputs inválido. Esperado: " + this.inputNeuronCount
            );
        }

        // Entradas (sem o neurônio de bias)
        for (int i = 0; i < this.inputNeuronCount; i++) {
            this.inputLayer.neurons[i].output = inputs[i];
        }

        Layer previous = this.inputLayer;

        // Camadas ocultas
        for (int k = 0; k < this.hiddenLayerCount; k++) {
            Layer current = this.hiddenLayers[k];

            for (int i = 0; i < current.computableCount; i++) {
                double sum = 0.0;
                for (int j = 0; j < previous.neuronCount; j++) {
                    sum += previous.neurons[j].output * current.neurons[i].weights[j];
                }
                current.neurons[i].output = relu(sum);
            }
            previous = current;
        }

        // Saída
        double[] outputs = new double[this.outputNeuronCount];
        for (int i = 0; i < this.outputNeuronCount; i++) {
            double sum = 0.0;
            for (int j = 0; j < previous.neuronCount; j++) {
                sum += previous.neurons[j].output * this.outputLayer.neurons[i].weights[j];
            }
            outputs[i] = applyOut(sum, outAct[i]);
            this.outputLayer.neurons[i].output = outputs[i];
        }
        return outputs;
    }

    // ---- Backprop (MSE) ----
    // Treina por épocas com mini-batches. l2 é regularização (pode ser 0).
    public void train(double[][] X, double[][] Y, int epochs, int batchSize, double lr, double l2) {
        if (X.length != Y.length) throw new IllegalArgumentException("X e Y com tamanhos diferentes.");
        int N = X.length;
        if (N == 0) return;
        if (batchSize <= 0) batchSize = N;

        // buffers de gradiente
        double[][][] gHiddenW = new double[hiddenLayerCount][][];
        for (int k = 0; k < hiddenLayerCount; k++) {
            Layer L = hiddenLayers[k];
            gHiddenW[k] = new double[L.computableCount][L.neurons[0].weights.length];
        }
        double[][] gOutW = new double[outputNeuronCount][outputLayer.neurons[0].weights.length];

        // deltas
        double[][] deltaHidden = new double[hiddenLayerCount][];
        for (int k = 0; k < hiddenLayerCount; k++) {
            deltaHidden[k] = new double[hiddenLayers[k].computableCount];
        }
        double[] deltaOut = new double[outputNeuronCount];

        int[] idx = new int[N];
        for (int i = 0; i < N; i++) idx[i] = i;

        for (int ep = 1; ep <= epochs; ep++) {
            // embaralhar
            shuffle(idx);

            double epochLoss = 0.0;

            for (int start = 0; start < N; start += batchSize) {
                int end = Math.min(N, start + batchSize);
                int B = end - start;

                // zera gradientes do batch
                zero(gOutW);
                for (int k = 0; k < hiddenLayerCount; k++) zero(gHiddenW[k]);

                for (int b = start; b < end; b++) {
                    int s = idx[b];

                    // forward
                    double[] yhat = feedForward(X[s]);
                    // loss MSE sobre saídas ativadas
                    epochLoss += mse(yhat, Y[s]);

                    // deltas saída
                    for (int i = 0; i < outputNeuronCount; i++) {
                        double dAct = dOutFromOutput(yhat[i], outAct[i]);
                        deltaOut[i] = (yhat[i] - Y[s][i]) * dAct;
                    }

                    // backprop ocultas (do último para o primeiro)
                    Layer next = outputLayer;
                    // primeiro, propagar para a última oculta
                    int last = hiddenLayerCount - 1;
                    Layer lastHidden = hiddenLayers[last];
                    for (int j = 0; j < lastHidden.computableCount; j++) {
                        double sum = 0.0;
                        for (int i = 0; i < outputNeuronCount; i++) {
                            sum += next.neurons[i].weights[j] * deltaOut[i];
                        }
                        double d = dreluFromOut(lastHidden.neurons[j].output);
                        deltaHidden[last][j] = sum * d;
                    }
                    // ocultas anteriores
                    for (int k = hiddenLayerCount - 2; k >= 0; k--) {
                        Layer curr = hiddenLayers[k];
                        Layer nxt  = hiddenLayers[k + 1];
                        for (int j = 0; j < curr.computableCount; j++) {
                            double sum = 0.0;
                            for (int i = 0; i < nxt.computableCount; i++) {
                                sum += nxt.neurons[i].weights[j] * deltaHidden[k + 1][i];
                            }
                            double d = dreluFromOut(curr.neurons[j].output);
                            deltaHidden[k][j] = sum * d;
                        }
                    }

                    // gradientes: saída
                    Layer prev = hiddenLayers[hiddenLayerCount - 1];
                    for (int i = 0; i < outputNeuronCount; i++) {
                        double[] w = gOutW[i];
                        for (int j = 0; j < prev.neuronCount; j++) {
                            w[j] += deltaOut[i] * prev.neurons[j].output;
                        }
                    }
                    // gradientes: ocultas
                    for (int k = 0; k < hiddenLayerCount; k++) {
                        Layer curr = hiddenLayers[k];
                        Layer prv  = (k == 0) ? inputLayer : hiddenLayers[k - 1];
                        for (int i = 0; i < curr.computableCount; i++) {
                            double[] w = gHiddenW[k][i];
                            for (int j = 0; j < prv.neuronCount; j++) {
                                w[j] += deltaHidden[k][i] * prv.neurons[j].output;
                            }
                        }
                    }
                } // fim do batch

                double invB = 1.0 / B;

                // atualiza pesos com L2
                // saída
                for (int i = 0; i < outputNeuronCount; i++) {
                    double[] w = outputLayer.neurons[i].weights;
                    double[] gW = gOutW[i];
                    for (int j = 0; j < w.length; j++) {
                        double grad = gW[j] * invB + l2 * w[j];
                        w[j] -= lr * grad;
                    }
                }
                // ocultas
                for (int k = 0; k < hiddenLayerCount; k++) {
                    for (int i = 0; i < hiddenLayers[k].computableCount; i++) {
                        double[] w = hiddenLayers[k].neurons[i].weights;
                        double[] gW = gHiddenW[k][i];
                        for (int j = 0; j < w.length; j++) {
                            double grad = gW[j] * invB + l2 * w[j];
                            w[j] -= lr * grad;
                        }
                    }
                }
            } // fim época

            System.out.printf("Epoch %d  | loss=%.5f%n", ep, epochLoss / N);
        }
    }

    // ---- Vetorização de pesos (inalterado) ----
    public int getTotalWeightCount() {
        int sum = 0;
        for (int k = 0; k < this.hiddenLayerCount; k++) {
            Layer L = this.hiddenLayers[k];
            for (int i = 0; i < L.computableCount; i++) {
                sum += L.neurons[i].weights.length;
            }
        }
        for (int i = 0; i < this.outputNeuronCount; i++) {
            sum += this.outputLayer.neurons[i].weights.length;
        }
        return sum;
    }

    public double[] getWeightsAsVector() {
        double[] weightsVector = new double[this.getTotalWeightCount()];
        int index = 0;
        for (int k = 0; k < this.hiddenLayerCount; k++) {
            Layer L = this.hiddenLayers[k];
            for (int i = 0; i < L.computableCount; i++) {
                double[] w = L.neurons[i].weights;
                System.arraycopy(w, 0, weightsVector, index, w.length);
                index += w.length;
            }
        }
        for (int i = 0; i < this.outputNeuronCount; i++) {
            double[] w = this.outputLayer.neurons[i].weights;
            System.arraycopy(w, 0, weightsVector, index, w.length);
            index += w.length;
        }
        return weightsVector;
    }

    public void setWeightsFromVector(double[] weightsVector) {
        if (weightsVector == null || weightsVector.length != this.getTotalWeightCount()) {
            throw new IllegalArgumentException("Vetor de pesos com tamanho incorreto.");
        }
        int index = 0;
        for (int k = 0; k < this.hiddenLayerCount; k++) {
            Layer L = this.hiddenLayers[k];
            for (int i = 0; i < L.computableCount; i++) {
                double[] w = L.neurons[i].weights;
                System.arraycopy(weightsVector, index, w, 0, w.length);
                index += w.length;
            }
        }
        for (int i = 0; i < this.outputNeuronCount; i++) {
            double[] w = this.outputLayer.neurons[i].weights;
            System.arraycopy(weightsVector, index, w, 0, w.length);
            index += w.length;
        }
    }

    // ---- util ----
    private static double applyOut(double z, int act) {
        if (act == ACT_TANH) return tanh(z);
        if (act == ACT_SIGM) return sigmoid(z);
        return z; // identity
    }
    private static double dOutFromOutput(double y, int act) {
        if (act == ACT_TANH) return dtanhFromOut(y);
        if (act == ACT_SIGM) return dsigmoidFromOut(y);
        return 1.0; // identity
    }
    private static double mse(double[] yhat, double[] y) {
        double s = 0.0;
        for (int i = 0; i < y.length; i++) {
            double d = yhat[i] - y[i];
            s += d * d;
        }
        return s / y.length;
    }
    private static void shuffle(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = RAND.nextInt(i + 1);
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }
    private static void zero(double[][] m) {
        for (double[] r : m) java.util.Arrays.fill(r, 0.0);
    }

    // ---- Estruturas internas ----
    private static class Layer {
        final Neuron[] neurons;
        final int neuronCount;       // inclui bias se hasBias = true
        final boolean hasBias;
        final int computableCount;   // exclui o bias se existir

        Layer(int neuronCount, int connectionsPerNeuron, boolean hasBias) {
            this.neuronCount     = neuronCount;
            this.hasBias         = hasBias;
            this.computableCount = hasBias ? neuronCount - 1 : neuronCount;
            this.neurons         = new Neuron[neuronCount];

            for (int i = 0; i < neuronCount; i++) {
                int conns = (hasBias && i == neuronCount - 1) ? 0 : Math.max(0, connectionsPerNeuron);
                this.neurons[i] = new Neuron(conns);
            }
            if (hasBias) {
                this.neurons[neuronCount - 1].output = 1.0; // bias
            }
        }
    }

    private static class Neuron {
        double[] weights;
        double output;

        Neuron(int connectionCount) {
            this.weights = new double[connectionCount];
            this.output  = 0.0;
            randomizeWeights();
        }
        void randomizeWeights() {
            double std = 0.5;
            for (int i = 0; i < this.weights.length; i++) {
                this.weights[i] = RAND.nextGaussian() * std;
            }
        }
    }

    public void mutate(double mutationRate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mutate'");
    }
    public RedeNeural crossover(RedeNeural p2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'crossover'");
    }
}
