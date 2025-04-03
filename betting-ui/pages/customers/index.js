import { useEffect, useState } from "react";
import withAuth from "../../auth/middleware/withAuth";
import buildClient from "../../api/build-client";
import { useKeycloak } from "../../auth/provider/KeycloakProvider";
import { v4 as uuidv4 } from 'uuid';

const CustomerList = () => {
  const [customers, setCustomers] = useState([]);
  const { user } = useKeycloak();

  useEffect(() => {
    if (user) {
      const fetchCustomers = async () => {
        const client = buildClient({ req: {}, currentUser: user });
        const { data } = await client.get("/api/customer/all");
        setCustomers(data);

        const existingCustomer = data.find(c => c.walletId === user.name);

        if (!existingCustomer) {
          const requestId = uuidv4();
          await client.post(`/api/customer/register/${user.name}/${requestId}`);

          // Refetch updated list
          const { data: updatedData } = await client.get("/api/customer/all");
          setCustomers(updatedData);
        }
      };
      fetchCustomers();
    }
  }, [user]);

  return (
      <div>
        <h1>Players</h1>
        <table className="table">
          <thead>
          <tr>
            <th>Username</th>
            <th>Balance</th>
          </tr>
          </thead>
          <tbody>
          {customers.map(customer => (
              <tr key={customer.walletId}>
                <td>{customer.walletId}</td>
                <td>{customer.balance}</td>
              </tr>
          ))}
          </tbody>
        </table>
      </div>
  );
};

export default withAuth(CustomerList);