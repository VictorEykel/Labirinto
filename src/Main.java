import maze.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SISTEMA DE LABIRINTO COM MÚLTIPLOS RATOS ===\n");

        // Cria o gerador de labirinto
        MazeGenerator generator = new MazeGenerator();

        // Gera um labirinto 21x21 (tamanho maior para mais espaço)
        Maze maze = generator.generateMaze(21, 21);

        int valRato = 3;
        int velocidadeRato = 500;

        // Adiciona múltiplos ratos ao labirinto
        maze.addMice(valRato, velocidadeRato); // Adiciona 5 ratos

        // Exibe o labirinto inicial
        maze.display();

        // Aguarda um momento para visualizar o estado inicial
        Thread.sleep(2000);

        maze.play(400);

        // Mantém o programa rodando
        Thread.currentThread().join();
    }
}