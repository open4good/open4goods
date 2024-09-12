db.getSiblingDB('admin').auth(
    'root',
    'rootpassword'
);

db.createUser(
    {
        user: "nudger",
        pwd: "nudger",
        roles: [
            {
                role: "readWrite",
                db: "products"
            }
        ]
    }
);
