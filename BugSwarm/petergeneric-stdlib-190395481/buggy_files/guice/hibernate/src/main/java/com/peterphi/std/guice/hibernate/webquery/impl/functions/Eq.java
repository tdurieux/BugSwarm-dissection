package com.peterphi.std.guice.hibernate.webquery.impl.functions;

import com.peterphi.std.guice.hibernate.webquery.impl.QFunction;
import com.peterphi.std.guice.hibernate.webquery.impl.QPropertyRef;
import com.peterphi.std.guice.hibernate.webquery.impl.QSizeProperty;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

class Eq implements QFunction
{
	private final QPropertyRef property;
	private final Object value;


	public Eq(final QPropertyRef property, final String value)
	{
		this.property = property;
		this.value = property.parseValue(value);
	}


	@Override
	public Criterion encode()
	{
		if (property.getProperty() instanceof QSizeProperty)
		{
			final int val = (Integer) value;

			if (val == 0)
				return Restrictions.isEmpty(property.getName());
			else
				return Restrictions.sizeEq(property.getName(), (Integer) value);
		}
		else
			return Restrictions.eq(property.getName(), value);
	}
}
