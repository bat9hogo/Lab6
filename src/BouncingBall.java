import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
public class BouncingBall implements Runnable {
    // Максимальный радиус, который может иметь мяч
    private static final int MAX_RADIUS = 40;
    // Минимальный радиус, который может иметь мяч
    private static final int MIN_RADIUS = 3;
    // Максимальная скорость, с которой может летать мяч
    private static final int MAX_SPEED = 15;
    private Field field;
    private int radius;
    private Color color;
    // Текущие координаты мяча
    private double x;
    private double y;
    // Вертикальная и горизонтальная компонента скорости
    private int speed;
    private double speedX;
    private double speedY;
    private volatile boolean isAlive = true;
    private double rotationSpeed; // Скорость поворота
    private double currentAngle = 0; // Текущий угол поворота

    // Конструктор класса BouncingBall
    public BouncingBall(Field field) {
// Необходимо иметь ссылку на поле, по которому прыгает мяч,
// чтобы отслеживать выход за его пределы
// через getWidth(), getHeight()
        this.field = field;
// Радиус мяча случайного размера
        this.radius = new Double(Math.random()*(MAX_RADIUS - MIN_RADIUS)).intValue() + MIN_RADIUS;

// Абсолютное значение скорости зависит от диаметра мяча,
// чем он больше, тем медленнее
        this.speed = new Double(Math.round(5*MAX_SPEED / radius)).intValue();
        if (speed>MAX_SPEED) {
            speed = MAX_SPEED;
        }
// Начальное направление скорости тоже случайно,
// угол в пределах от 0 до 2PI
        double angle = Math.random()*2*Math.PI;
// Вычисляются горизонтальная и вертикальная компоненты скорости
        this.speedX = 3*Math.cos(angle);
        this.speedY = 3*Math.sin(angle);
// Цвет мяча выбирается случайно
        this.color = new Color((float)Math.random(), (float)Math.random(),
                (float)Math.random());
// Начальное положение мяча случайно
        this.x = Math.random()*(field.getSize().getWidth()-2*radius) + radius;
        this.y = Math.random()*(field.getSize().getHeight()-2*radius) + radius;
// Создаѐм новый экземпляр потока, передавая аргументом
// ссылку на класс, реализующий Runnable (т.е. на себя)
        this.rotationSpeed = 100.0/radius;
        if (Math.random() < 0.5) {
            this.rotationSpeed *= -1;
        }
        Thread thisThread = new Thread(this);
// Запускаем поток
        thisThread.start();
    }

    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }
    public int getRadius(){
        return radius;
    }
    public void stop(){
        isAlive = false;
    }
    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }
    public BouncingBall(Field field, BouncingBall ball) {
        this.field = field;
        this.radius = ball.radius;  // Копируем радиус
        this.speed = ball.speed;  // Копируем скорость (но она будет зависеть от радиуса)

        // Генерируем случайное направление скорости для клонированного мяча
        double angle = Math.random() * 2 * Math.PI;
        this.speedX = 3 * Math.cos(angle);  // Случайная горизонтальная скорость
        this.speedY = 3 * Math.sin(angle);  // Случайная вертикальная скорость

        this.color = ball.color;  // Копируем цвет
        this.x = ball.x;  // Копируем позицию
        this.y = ball.y;

        this.rotationSpeed = ball.rotationSpeed;
        this.currentAngle = ball.currentAngle;

        new Thread(this).start();
    }

    // Метод run() исполняется внутри потока. Когда он завершает работу,
// то завершается и поток
    public void run() {
        try {
// Крутим бесконечный цикл, т.е. пока нас не прервут,
// мы не намерены завершаться
            while(isAlive) {
// Синхронизация потоков на самом объекте поля
// Если движение разрешено - управление будет
// возвращено в метод
// В противном случае - активный поток заснѐт
                field.canMove(this);
                currentAngle += rotationSpeed;
                if (currentAngle >= 360) {
                    currentAngle -= 360;
                }

                if (x + speedX <= radius) {
// Достигли левой стенки, отскакиваем право
                    speedX = -speedX;
                    x = radius;
                } else
                if (x + speedX >= field.getWidth() - radius) {
// Достигли правой стенки, отскок влево
                    speedX = -speedX;
                    x=new Double(field.getWidth()-radius).intValue();
                } else
                if (y + speedY <= radius) {
// Достигли верхней стенки
                    speedY = -speedY;
                    y = radius;
                } else
                if (y + speedY >= field.getHeight() - radius) {
// Достигли нижней стенки
                    speedY = -speedY;
                    y=new Double(field.getHeight()-radius).intValue();
                } else {
// Просто смещаемся
                    field.checkZones(this);
                    x += speedX;
                    y += speedY;
                }
// Засыпаем на X миллисекунд, где X определяется
// исходя из скорости
// Скорость = 1 (медленно), засыпаем на 15 мс.
// Скорость = 15 (быстро), засыпаем на 1 мс.
                Thread.sleep(16-speed);
            }
        } catch (InterruptedException ex) {


// Если нас прервали, то ничего не делаем
// и просто выходим (завершаемся)
        }
    }

    public void paint(Graphics2D canvas) {
        canvas.setColor(color);
        canvas.setPaint(color);

        // Вычисляем длину стороны треугольника
        double side = Math.sqrt(3) * radius;

        // Координаты вершин равностороннего треугольника относительно центра
        double[] xOffsets = {0, -side / 2, side / 2};
        double[] yOffsets = {-radius, radius / 2, radius / 2};

        // Вычисляем координаты вершин с учетом поворота
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        for (int i = 0; i < 3; i++) {
            double rotatedX = xOffsets[i] * Math.cos(Math.toRadians(currentAngle)) -
                    yOffsets[i] * Math.sin(Math.toRadians(currentAngle));
            double rotatedY = xOffsets[i] * Math.sin(Math.toRadians(currentAngle)) +
                    yOffsets[i] * Math.cos(Math.toRadians(currentAngle));
            xPoints[i] = (int) (x + rotatedX);
            yPoints[i] = (int) (y + rotatedY);
        }

        // Рисуем треугольник
        canvas.fillPolygon(xPoints, yPoints, 3);
    }


}
