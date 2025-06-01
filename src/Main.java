import maze.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SISTEMA DE LABIRINTO ===\n");

        // Cria o gerador de labirinto
        MazeGenerator generator = new MazeGenerator();

        // Gera um labirinto 15x15
        System.out.println("Gerando labirinto...");
        Maze maze = generator.generateMaze(15, 15);

        // Exibe informações do labirinto
        maze.printInfo();

        // Exibe o labirinto inicial
        maze.display();

        // Inicia a simulação com velocidade de 500ms entre movimentos
        System.out.println("Iniciando simulação...");
        maze.play(500);

        // Mantém o programa rodando
        Thread.currentThread().join();
    }
}