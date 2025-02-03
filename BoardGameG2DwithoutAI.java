package BoardGameG2DPlayervsPlayer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

enum Piece {
    NONE, PLAYER, AI
}

enum GameState {
    AI_FIRST_TURN, AI_SECOND_TURN, PLAYER_FIRST_TURN, PLAYER_SECOND_TURN
}

public class BoardGameG2DwithoutAI extends JPanel implements MouseListener {
    private Piece[][] grid;
    private int cellSize;
    private int moveCount = 0;
    private GameState currentState;
    private String gameResult;
    private boolean gameOver = false;
    private Point selectedPiece;
    private Point lastMovedPiece;
    private boolean captureOccurred = false; // Flag to track captures
    private int playerMovesMade = 0; // Track player's moves per turn

    // Highlight colors
    private final Color SELECTION_COLOR = new Color(255, 255, 0, 100);
    private final Color LAST_MOVE_COLOR = new Color(0, 255, 0, 100);
    private final Color VALID_MOVE_COLOR = new Color(0, 255, 0, 50);

    public BoardGameG2DwithoutAI(int cellSize) {
        this.cellSize = cellSize;
        this.grid = new Piece[7][7];
        this.currentState = GameState.AI_FIRST_TURN;
        this.selectedPiece = null;
        this.lastMovedPiece = null;
        initGrid();
        addMouseListener(this);
    }

    private void initGrid() {
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                grid[row][col] = Piece.NONE;
            }
        }

        grid[0][0] = Piece.AI;
        grid[2][0] = Piece.AI;
        grid[4][6] = Piece.AI;
        grid[6][6] = Piece.AI;

        grid[0][6] = Piece.PLAYER;
        grid[2][6] = Piece.PLAYER;
        grid[4][0] = Piece.PLAYER;
        grid[6][0] = Piece.PLAYER;
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (toRow < 0 || toRow >= 7 || toCol < 0 || toCol >= 7) {
            return false;
        }

        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);

        return (rowDiff + colDiff) == 1 && grid[toRow][toCol] == Piece.NONE;
    }

    private int countPieces(Piece player) {
        int count = 0;
        for (Piece[] row : grid) {
            for (Piece piece : row) {
                if (piece == player) {
                    count++;
                }
            }
        }
        return count;
    }

    private void checkCaptures() {
        List<Point> toRemove = new ArrayList<>();

        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                if (grid[row][col] != Piece.NONE) {
                    Piece currentPiece = grid[row][col];
                    Piece opponentPiece = (currentPiece == Piece.PLAYER) ? Piece.AI : Piece.PLAYER;

                    // Check for Wall Captures in all directions
                    checkWallCapture(row, col, -1, 0, opponentPiece, toRemove); // Up
                    checkWallCapture(row, col, 1, 0, opponentPiece, toRemove);  // Down
                    checkWallCapture(row, col, 0, -1, opponentPiece, toRemove); // Left
                    checkWallCapture(row, col, 0, 1, opponentPiece, toRemove);  // Right

                    // Check for Sandwich Captures in horizontal and vertical directions
                    checkSandwichCapture(row, col, 1, 0, toRemove);  // Vertical (Down)
                    checkSandwichCapture(row, col, -1, 0, toRemove); // Vertical (Up)
                    checkSandwichCapture(row, col, 0, 1, toRemove);  // Horizontal (Right)
                    checkSandwichCapture(row, col, 0, -1, toRemove); // Horizontal (Left)
                }
            }
        }

        // Remove all captured pieces
        for (Point p : toRemove) {
            grid[p.x][p.y] = Piece.NONE;
        }

        if (!toRemove.isEmpty()) {
            captureOccurred = true;
        }
    }

    private void checkWallCapture(int row, int col, int dRow, int dCol, Piece opponentPiece, List<Point> toRemove) {
        int r = row + dRow;
        int c = col + dCol;

        List<Point> potentialCaptures = new ArrayList<>();
        while (r >= 0 && r < 7 && c >= 0 && c < 7 && grid[r][c] == opponentPiece) {
            potentialCaptures.add(new Point(r, c));
            r += dRow;
            c += dCol;
        }

        // Check if the series of opponent pieces ends at a boundary or same-player piece
        if ((r < 0 || r >= 7 || c < 0 || c >= 7) || (r >= 0 && r < 7 && c >= 0 && c < 7 && grid[r][c] == grid[row][col])) {
            toRemove.addAll(potentialCaptures);
        }
    }

    private void checkSandwichCapture(int row, int col, int dRow, int dCol, List<Point> toRemove) {
        Piece currentPiece = grid[row][col];
        Piece opponentPiece = (currentPiece == Piece.PLAYER) ? Piece.AI : Piece.PLAYER;

        int r = row + dRow;
        int c = col + dCol;

        List<Point> potentialCaptures = new ArrayList<>();

        while (r >= 0 && r < 7 && c >= 0 && c < 7) {
            if (grid[r][c] == opponentPiece) {
                potentialCaptures.add(new Point(r, c));
            } else if (grid[r][c] == currentPiece) {
                // Found the same player's piece, confirm capture
                toRemove.addAll(potentialCaptures);
                return;
            } else {
                // Empty cell or invalid, stop checking
                break;
            }

            r += dRow;
            c += dCol;
        }
    }

    private void checkGameCondition() {
        int aiCount = countPieces(Piece.AI);
        int playerCount = countPieces(Piece.PLAYER);

        if (aiCount == 0 && playerCount == 0) {
            gameResult = "Draw: Both players have no pieces left!";
            gameOver = true;
        } else if (aiCount == 0) {
            gameResult = "Player Wins!";
            gameOver = true;
        } else if (playerCount == 0) {
            gameResult = "AI Wins!";
            gameOver = true;
        } else if (moveCount >= 50) {
            gameResult = (playerCount > aiCount) ? "Player Wins after 50 moves!" :
                    (playerCount < aiCount) ? "AI Wins after 50 moves!" : "Draw after 50 moves!";
            gameOver = true;
        }
    }

    private void handleMove(int row, int col) {
        Piece currentPlayer = (currentState == GameState.PLAYER_FIRST_TURN || currentState == GameState.PLAYER_SECOND_TURN)
                ? Piece.PLAYER : Piece.AI;

        int remainingPieces = countPieces(currentPlayer);
        int maxMoves = (remainingPieces > 1) ? 2 : 1; // Allow 2 moves if more than 1 piece, otherwise 1

        if (selectedPiece == null) {
            if (grid[row][col] == currentPlayer && (lastMovedPiece == null || !lastMovedPiece.equals(new Point(row, col)))) {
                selectedPiece = new Point(row, col);
                repaint();
            }
        } else {
            if (isValidMove(selectedPiece.x, selectedPiece.y, row, col)) {
                // Move the piece
                grid[row][col] = grid[selectedPiece.x][selectedPiece.y];
                grid[selectedPiece.x][selectedPiece.y] = Piece.NONE;

                lastMovedPiece = new Point(row, col);
                moveCount++;
                playerMovesMade++;

                // Re-check all capture scenarios after the move
                checkCaptures();

                // End the turn after max moves are made
                if (playerMovesMade >= maxMoves) {
                    currentState = (currentState == GameState.PLAYER_FIRST_TURN || currentState == GameState.PLAYER_SECOND_TURN)
                            ? GameState.AI_FIRST_TURN : GameState.PLAYER_FIRST_TURN;
                    playerMovesMade = 0;
                }

                selectedPiece = null;
                checkGameCondition(); // Check if the game has ended after captures
                repaint(); // Repaint to reflect changes immediately
            } else {
                selectedPiece = null; // Deselect invalid move
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                int x = col * cellSize;
                int y = row * cellSize;

                g2d.setColor(Color.GRAY);
                g2d.drawRect(x, y, cellSize, cellSize);

                // Highlight selection
                if (selectedPiece != null && row == selectedPiece.x && col == selectedPiece.y) {
                    g2d.setColor(SELECTION_COLOR);
                    g2d.fillRect(x, y, cellSize, cellSize);
                }

                // Highlight last move
                if (lastMovedPiece != null && row == lastMovedPiece.x && col == lastMovedPiece.y) {
                    g2d.setColor(LAST_MOVE_COLOR);
                    g2d.fillRect(x, y, cellSize, cellSize);
                }

                // Highlight valid moves
                if (selectedPiece != null) {
                    List<Point> validMoves = getValidMoves(selectedPiece.x, selectedPiece.y);
                    if (validMoves.contains(new Point(row, col))) {
                        g2d.setColor(VALID_MOVE_COLOR);
                        g2d.fillRect(x, y, cellSize, cellSize);
                    }
                }

                if (grid[row][col] == Piece.AI) {
                    g2d.setColor(Color.RED);
                    g2d.fillPolygon(new int[]{x + cellSize / 2, x + 10, x + cellSize - 10},
                            new int[]{y + 10, y + cellSize - 10, y + cellSize - 10}, 3);
                } else if (grid[row][col] == Piece.PLAYER) {
                    g2d.setColor(Color.BLUE);
                    g2d.fillOval(x + 10, y + 10, cellSize - 20, cellSize - 20);
                }
            }
        }

        // Draw game information on the right side
        int infoX = 750; // Starting x-coordinate for information
        int infoY = 50;  // Starting y-coordinate for information
        int lineHeight = 30; // Line spacing

        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.setColor(Color.BLACK);
        infoY += lineHeight;
        g2d.drawString("Move Count: " + moveCount, infoX, infoY);
        infoY += lineHeight;
        g2d.drawString("Player Pieces: " + countPieces(Piece.PLAYER), infoX, infoY);
        infoY += lineHeight;
        g2d.drawString("AI Pieces: " + countPieces(Piece.AI), infoX, infoY);

        if (gameOver) {
            infoY += lineHeight;
            g2d.setColor(Color.RED);
            g2d.drawString("Game Over!", infoX, infoY);
            infoY += lineHeight;
            g2d.drawString("Result: " + gameResult, infoX, infoY);
        }
    }

    private List<Point> getValidMoves(int row, int col) {
        List<Point> validMoves = new ArrayList<>();
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (isValidMove(row, col, newRow, newCol)) {
                validMoves.add(new Point(newRow, newCol));
            }
        }
        return validMoves;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int col = e.getX() / cellSize;
        int row = e.getY() / cellSize;

        if (!gameOver) handleMove(row, col);
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("7x7 Board Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BoardGameG2DwithoutAI board = new BoardGameG2DwithoutAI(100);
        frame.add(board);
        frame.setSize(930, 740); // Adjust size to accommodate the info panel
        frame.setLocationRelativeTo(null); // Center the frame on the screen
        frame.setVisible(true);
    }
}

