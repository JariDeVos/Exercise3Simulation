import java.util.Random;

public class Distributions {

    public static double Exponential_distribution (double lambda, Random r){
        double j1 = (float) r.nextInt(1000+1)/1000;
        if (j1 == 0)
            j1 += 0.0001;
        double j2 = -Math.log(j1)/lambda;
        return j2;
    }

    public static int Poisson_distribution(double lambda, Random r){
        double j1 = (float) r.nextInt(1000+1)/1000;
        int k = 0;
        double L = Math.exp(-lambda);
        double j3 = 0;
        int p,i;
        double j2;
        do{
            j2 = L * Math.pow(lambda, k);
            p = 1;
            for (i = 0; i <= k; i++){   if (i == 0)
                p = 1;
            else
                p *= i;
            }
            j2 /= p;
            j3 += j2;
            k++;
        } while (j1 >= j3);

        return k-1;
    }

    public static int  Normal_distribution(double mean, double stdev, Random r){   // TO MODEL BASED ON CUMULATIVE DENSITY FUNCTION OF NORMAL DISTRIBUTION BASED ON BOOK OF SHELDON ROSS, Simulation, The polar method, p80.
        double v1, v2, t;
        do{
            v1 = (float) r.nextInt(1000+1)*2;
            v1 /= 1000;
            v1 -= 1;
            v2 = (float) r.nextInt(1000+1)*2;
            v2 /= 1000;
            v2 -= 1;
            t=v1*v1+v2*v2;
        }
        while(t>=1||t==0);
        double multiplier = Math.sqrt(-2*Math.log(t)/t);
        return (int) (v1 * multiplier * stdev + mean);
    }

    public static int  Bernouilli_distribution(double prob, Random r){     // INVERSION METHOD BERNOUILLI DISTRIBUTION
        double j1 = (float) r.nextInt(1000+1)/1000;
        if (j1 < prob)
            return 0;
        else
            return 1;
    }

    public static int  Uniform_distribution(double a, double b, Random r){ // INVERSION METHOD UNIFORM DISTRIBUTION
        double j1 = (float) r.nextInt(1000+1)/1000;
        return (int) (a + (b-a) * j1);
    }


    public static int  Triangular_distribution(int a, int b, int c, Random r){ // INVERSION METHOD TRIANGULAR DISTRIBUTION
        double mean, stdev;
        double x, L;

        mean = (a+b+c)/3;
        stdev = (Math.pow(a,2)+Math.pow(b,2)+Math.pow(c,2)-a*b-a*c-b*c)/18;
        stdev = Math.sqrt(stdev);
        double j1 = (float) r.nextInt(1000+1)/1000;
        x = a;

        do
        {   if (x <= b)
            L = Math.pow((x-a),2)/((c-a)*(b-a));
        else
            L = 1-(Math.pow(c-x,2)/((c-a)*(c-b)));
            x++;
        } while (j1 >= L);

        return (int) x-1;
    }


}
