-- ===========================================================================
--  Demo data for the company ontology.
--
--  The graph the ontology sees is *derived* from these rows by Ontop:
--    department -> :dept/{id} a :Department, :departmentName, :partOf
--    employee   -> :emp/{id}  a :Employee,   :name, :salary, :startDate,
--                               :worksIn, :managedBy
--    project    -> :proj/{id} a :Project,    :projectName, :budget, :ownedBy
--    assignment -> :emp/{employee_id} :assignedTo :proj/{project_id}
--
--  Nothing in this file mentions OWL/RDF: that is the point of OBDA.
-- ===========================================================================

INSERT INTO department (id, name, parent_id, head_id) VALUES
    (1, 'Engineering',  5,    2),
    (2, 'Platform',     1,    3),
    (3, 'Data',         1,    4),
    (4, 'Finance',      NULL, 9),
    (5, 'Technology',   NULL, 1);

INSERT INTO employee (id, name, salary, start_date, department_id, manager_id) VALUES
    (1,  'Alice', 42000.00, '2018-01-15', 5,    NULL),
    (2,  'Bob',   30000.00, '2019-03-01', 1,    1),
    (3,  'Carol', 31000.00, '2019-07-15', 2,    2),
    (4,  'Dave',  28000.00, '2020-02-10', 3,    2),
    (5,  'Eve',   16000.00, '2021-06-01', 2,    3),
    (6,  'Frank', 19500.00, '2020-09-01', 2,    3),
    (7,  'Grace', 17500.00, '2022-01-10', 3,    4),
    (8,  'Heidi', 22000.00, '2021-03-20', 3,    4),
    (9,  'Ivan',  14000.00, '2022-08-01', 4,    NULL),
    (10, 'Judy',  15000.00, '2023-02-01', 1,    2);

INSERT INTO project (id, name, budget, department_id) VALUES
    (1, 'Atlas',         250000.00, 2),
    (2, 'Borealis',      180000.00, 3),
    (3, 'Core Platform', 420000.00, 1),
    (4, 'Ledger',         90000.00, 4);

INSERT INTO assignment (employee_id, project_id) VALUES
    (2, 3),
    (3, 2),
    (5, 1),
    (6, 1),
    (7, 2),
    (8, 2),
    (9, 4),
    (10, 3);
