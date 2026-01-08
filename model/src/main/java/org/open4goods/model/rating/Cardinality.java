package org.open4goods.model.rating;

import java.security.InvalidParameterException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains min / max / average /sum informations for a rating
 *
 * @author goulven
 *
 */
public class Cardinality {

	protected static final Logger logger = LoggerFactory.getLogger(Cardinality.class);

	
	private Double value;
	
	/**
	 * The minimum encountered
	 */
	private Double min = null;

	/**
	 * The maximum encountered
	 */
	private Double max = null;

	/**
	 * The average value
	 */
	private Double avg = null;

	/**
	 * The number of elements for computing this cardinality
	 */
	private Integer count = 0;

	/**
	 * The sum
	 */
	private Double sum = 0.0;



	/**
	 * The sum of squares for variance computation
	 */
	private Double sumOfSquares = 0.0;

	public Cardinality() {
	}

	public Cardinality(Cardinality source) {
		
		this.avg = source.avg;
		this.count=source.count;
		this.max=source.max;
		this.min=source.min;
		this.sum=source.sum;
		this.sumOfSquares = source.sumOfSquares;
		this.value=source.value;
	}


	@Override
	public String toString() {
		return min +" > " + avg + " < " +max+" (" + count+ " elements)";
	}

	
	public void increment(final Rating r) {
        if (r == null || r.getValue() == null) {
            return;
        }
		increment(r.getValue());
	}
		
	/**
	 * Increments rating cardinality with a
	 *
	 * @param r
	 * @throws InvalidParameterException
	 */
	public void increment(Double value) {

		try {
			// Min
			if (null == min || min > value) {
				min = value;
			}

			// Max
			if (null == max || max < value) {
				max = value;
			}

			// Count
			count++;

			// Sum
			sum += value;
            
            // Sum of squares
            sumOfSquares += value * value;

			// Average
			avg = sum / Double.valueOf(count);

		} catch (Exception e) {
			logger.error("Cardinality computation failed : {}", e.getMessage());
		}
	}
    
    /**
     * Computes the standard deviation of the distribution.
     * @return 0.0 if count is 0, otherwise the population standard deviation.
     */
    public Double getStdDev() {
        if (count == 0 || count == 1) {
            return 0.0;
        }
        
        // Variance = (SumSq - (Sum^2 / N)) / N
        double mean = sum / count;
        double variance = (sumOfSquares / count) - (mean * mean);
        
        // Handle floating point precision issues near zero
        return Math.sqrt(Math.max(0.0, variance)); 
    }

	public Double getMin() {
		return min;
	}

	public void setMin(Double min) {
		this.min = min;
	}

	public Double getMax() {
		return max;
	}

	public void setMax(Double max) {
		this.max = max;
	}

	public Double getAvg() {
		return avg;
	}

	public void setAvg(Double avg) {
		this.avg = avg;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public Double getSum() {
		return sum;
	}

	public void setSum(Double sum) {
		this.sum = sum;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}
	
	public Double getSumOfSquares() {
		return sumOfSquares;
	}

	public void setSumOfSquares(Double sumOfSquares) {
		this.sumOfSquares = sumOfSquares;
	}

}
