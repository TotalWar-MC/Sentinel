package org.mcmonkey.sentinel.totalwar;

import org.bukkit.util.Vector;

//a typical solid formation. strong anti-knockback, solid. have fun flanking individual troops unless you flank the whole formation/ or break it with a strong charge
//can volleyfire (will be able to)
public class LineFormation implements Formation {
	
	public static final LineFormation INSTANCE = new LineFormation();
	
	private LineFormation() {}
	
	//x is horizontal, z is vertical
	@Override
	public Vector[] form(int depth, int frontage, double spacing, SentinelRegiment regiment) {
		int N = regiment.getSoldiers().size();
		Vector[] ret = new Vector[N];
		double f = frontage == 0 ? Math.ceil((double)N/(double)depth) : frontage;
		for(int n = 0; n < N; n++) {
			double x = Math.ceil((double)(2 * (n % f) - f) / 2) * spacing;
			double z = n / f * spacing;
			ret[n] = new Vector(x, 0, z);
		}
		return ret;
	}
}
