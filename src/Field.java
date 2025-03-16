import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;

@SuppressWarnings("serial")
public class Field extends JPanel {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Флаг приостановленности движения
    private boolean paused;
    // Динамический список скачущих мячей
    private ArrayList<BouncingBall> balls = new ArrayList<BouncingBall>(10);
    // Класс таймер отвечает за регулярную генерацию событий ActionEvent
// При создании его экземпляра используется анонимный класс,
// реализующий интерфейс ActionListener

    private final Rectangle cloneZone = new Rectangle(200, 350, 100, 100);
    private final Rectangle removeZone = new Rectangle(1250, 350, 100, 100);
    private final Rectangle teleportZone = new Rectangle(700, 50, 100, 100);
    private final Rectangle destinationZone = new Rectangle(700, 600, 100, 100);

    private Timer repaintTimer = new Timer(10, new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
// Задача обработчика события ActionEvent - перерисовка окна
            repaint();
        }
    });
    // Конструктор класса BouncingBall
    public Field() {
// Установить цвет заднего фона белым
        setBackground(Color.BLACK);
// Запустить таймер
        repaintTimer.start();
    }
    // Унаследованный от JPanel метод перерисовки компонента
    public void paintComponent(Graphics g) {
// Вызвать версию метода, унаследованную от предка
        super.paintComponent(g);
        Graphics2D canvas = (Graphics2D) g;
        canvas.setColor(Color.GREEN);
        canvas.fill(cloneZone);
        canvas.setColor(Color.RED);
        canvas.fill(removeZone);
        canvas.setColor(Color.BLUE);
        canvas.fill(teleportZone);
        canvas.setColor(Color.YELLOW);
        canvas.fill(destinationZone);

        canvas.setColor(Color.WHITE);
        canvas.drawString("Clone Zone", cloneZone.x + 15, cloneZone.y + 20);
        canvas.drawString("Remove Zone", removeZone.x + 15, removeZone.y + 20);
        canvas.drawString("Teleport Zone", teleportZone.x + 10, teleportZone.y + 20);
        canvas.setColor(Color.BLACK);
        canvas.drawString("Destination", destinationZone.x + 15, destinationZone.y + 20);
// Последовательно запросить прорисовку от всех мячей из списка
        for (BouncingBall ball: balls) {
            ball.paint(canvas);
        }
    }
    // Метод добавления нового мяча в список
    public void addBall() {
//Заключается в добавлении в список нового экземпляра BouncingBall
// Всю инициализацию положения, скорости, размера, цвета
// BouncingBall выполняет сам в конструкторе
        balls.add(new BouncingBall(this));
    }

    public synchronized void removeBall(BouncingBall ball) {
        ball.stop();
        balls.remove(ball);
        clonedBalls.remove(ball);
    }

    // Метод синхронизированный, т.е. только один поток может
// одновременно быть внутри
    public synchronized void pause() {
// Включить режим паузы
        paused = true;
    }
    // Метод синхронизированный, т.е. только один поток может
// одновременно быть внутри
    public synchronized void resume() {
// Выключить режим паузы
        paused = false;
// Будим все ожидающие продолжения потоки
        notifyAll();
    }
    // Синхронизированный метод проверки, может ли мяч двигаться
// (не включен ли режим паузы?)
    public synchronized void canMove(BouncingBall ball) throws
            InterruptedException {
        if (paused) {
// Если режим паузы включен, то поток, зашедший
// внутрь данного метода, засыпает
            wait();
        }
    }

    private HashSet<BouncingBall> clonedBalls = new HashSet<>();

    public synchronized void checkZones(BouncingBall ball) {
        double ballLeft = ball.getX() - ball.getRadius();
        double ballRight = ball.getX() + ball.getRadius();
        double ballTop = ball.getY() - ball.getRadius();
        double ballBottom = ball.getY() + ball.getRadius();

        boolean inCloneZone = isFullyInside(ballLeft, ballRight, ballTop, ballBottom, cloneZone);

        if (inCloneZone && !clonedBalls.contains(ball)) {
            clonedBalls.add(ball); // Добавляем в список клонированных

            // Создаём клон в случайном месте
            double newX = Math.random() * (getWidth() - 2 * ball.getRadius()) + ball.getRadius();
            double newY = Math.random() * (getHeight() - 2 * ball.getRadius()) + ball.getRadius();

            BouncingBall newBall = new BouncingBall(this, ball);
            newBall.setPosition(newX, newY);
            addBall(newBall);
        }
        // Если мяч вышел из зоны, убираем его из списка клонированных
        else if (!inCloneZone) {
            clonedBalls.remove(ball);
        }

        if (isFullyInside(ballLeft, ballRight, ballTop, ballBottom, removeZone)) {
            removeBall(ball);
            clonedBalls.remove(ball);
        }
        if (isFullyInside(ballLeft, ballRight, ballTop, ballBottom, teleportZone)) {
            ball.setPosition(destinationZone.x + destinationZone.width / 2,
                    destinationZone.y + destinationZone.height / 2);
        }

    }

    private synchronized void addBall(BouncingBall newBall) {
        balls.add(newBall);
        // Запускаем поток для нового мяча
    }
    private boolean isFullyInside(double left, double right, double top, double bottom, Rectangle zone) {
        return left >= zone.x && right <= zone.x + zone.width &&
                top >= zone.y && bottom <= zone.y + zone.height;
    }
}

