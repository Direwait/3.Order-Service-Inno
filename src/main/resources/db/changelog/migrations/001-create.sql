CREATE TABLE IF NOT EXISTS orders(
	id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
	user_id UUID NOT NULL,
	status VARCHAR(64),
	total_price DECIMAL(10,2),
	deleted BOOLEAN DEFAULT FALSE,

	updated_at TIMESTAMP,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS items(
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(64),
    price DECIMAL(10,2),

    updated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items(
	id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
	order_id UUID REFERENCES orders(id),
	item_id UUID REFERENCES items(id),
	quantity INT,

	updated_at TIMESTAMP,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX IF NOT EXISTS items_name_ind ON items(name);
CREATE INDEX IF NOT EXISTS orders_status_ind ON orders(status);
CREATE INDEX IF NOT EXISTS orders_user_ind ON orders(user_id, deleted);

