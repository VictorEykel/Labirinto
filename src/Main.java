import maze.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SISTEMA DE LABIRINTO COM MÚLTIPLOS RATOS ===\n");

        // Cria o gerador de labirinto
        MazeGenerator generator = new MazeGenerator();

        // Gera um labirinto 21x21 (tamanho maior para mais espaço)
        System.out.println("Gerando labirinto...");
        Maze maze = generator.generateMaze(21, 21);

        // Adiciona múltiplos ratos ao labirinto
        System.out.println("Adicionando ratos ao labirinto...");
        maze.addMice(5); // Adiciona 5 ratos

        // Exibe informações do labirinto
        maze.printInfo();

        // Exibe o labirinto inicial
        maze.display();

        // Aguarda um momento para visualizar o estado inicial
        Thread.sleep(2000);

        // Inicia a simulação com velocidade de 400ms entre movimentos
        System.out.println("🎮 Iniciando corrida dos ratos!");
        System.out.println("Pressione Ctrl+C para parar a simulação\n");

        maze.play(400);

        // Mantém o programa rodando
        Thread.currentThread().join();
    }
}