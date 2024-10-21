# Trading Algorithm Assessment

## Objective
The objective of this project is to develop and test trading algorithms that can effectively analyze market conditions, make informed trading decisions, and manage orders efficiently. This assessment evaluates the algorithms' ability to handle various scenarios, including order placement, stop-loss and take-profit conditions,profit generation,  as well as the enforcement of maximum order limits.

---

## Components

### MyAlgoLogic
**MyAlgoLogic** is the core class that contains the trading logic for the algorithm. It evaluates market conditions and determines the appropriate actions to take based on the input data.

**Key Features:**
- **Dynamic Order Creation**: Automatically creates buy orders based on the best ask prices while considering market conditions and a defined maximum order count.
- **Mean Reversion Strategy**: Adjusts order prices based on mean reversion principles, ensuring orders are more likely to be filled.
- **Order Management**: Cancels the oldest active order when the maximum order count is reached, maintaining efficient order management.
- **Execution Handling**: Processes filled orders and manages take-profit and stop-loss conditions to secure profits and limit losses.
- **Logging and Monitoring**: Provides detailed logging of order states and actions for easier debugging and monitoring of algorithm performance.

---

### MyAlgoTest
**MyAlgoTest** contains the unit tests for **MyAlgoLogic**. It ensures that the logic is functioning as expected by testing various scenarios, including order creation, state transitions, and handling of filled orders.

**Key Features:**
- Tests various market scenarios to verify the correctness of trading logic.
- Validates stop-loss and take-profit functionality.
- Ensures that the maximum order limits are respected.

---

### MyAlgoBackTest
**MyAlgoBackTest** is the integration test suite that evaluates the entire backtesting framework. It simulates a trading environment and verifies the interactions between components.

**Key Features:**
- Simulates market ticks and tracks the state of child orders.
- Checks the behavior of stop-loss and take-profit mechanisms.
- Validates the system's ability to handle multiple active orders.

---

### StretchAlgoLogic
**StretchAlgoLogic** is another core class that implements additional trading logic for the algorithm, offering a different strategy based on market conditions.

**Key Features:**
- **Dynamic Order Creation**: Places buy orders using an iceberg order strategy, creating both visible and hidden parts of the order based on market conditions.
- **Risk Management**: Implements stop-loss and take-profit mechanisms to manage risk on active orders, ensuring potential losses are minimized and profits are secured.
- **Transaction Cost Management**: Takes transaction costs into account when calculating order costs, ensuring accurate available capital management.
- **Capital Management**: Tracks available capital and prevents orders from being placed if sufficient capital is not available.
- **Technical Indicators**: Utilizes Volume Weighted Average Price (VWAP) and Simple Moving Average (SMA) for making informed trading decisions based on market trends.
- **Logging and Monitoring**: Provides detailed logging of the order book state, trading decisions, and risk management actions for easier debugging and performance monitoring.

---

### StretchAlgoTest
**StretchAlgoTest** contains the unit tests for **StretchAlgoLogic**. It ensures that the logic is functioning as expected by testing various scenarios, including order creation, state transitions, and handling of filled orders.

**Key Features:**
- Tests various market scenarios to verify the correctness of trading logic.
- Validates stop-loss and take-profit functionality.
- Ensures that the maximum order limits are respected.

---

### StretchAlgoBackTest
**StretchAlgoBackTest** is the integration test suite that evaluates the entire backtesting framework for **StretchAlgoLogic**. It simulates a trading environment and verifies the interactions between components.

**Key Features:**
- Simulates market ticks and tracks the state of child orders.
- Checks the behavior of stop-loss and take-profit mechanisms.
- Validates the system's ability to handle multiple active orders.

---

## Prerequisites
- **Java Development Kit (JDK) 11 or higher**
- **Maven** (with the `mvnw` wrapper included)

---

## Setup Instructions
To build the project and install dependencies, run:
```bash
./mvnw clean install

## Run Tests
**To execute unit tests for MyAlgoTest, run:**
./mvnw -Dtest=MyAlgoTest test --projects algo-exercise/getting-started
./mvnw -Dtest=StretchAlgoTest test --projects algo-exercise/getting-started
./mvnw -Dtest=MyAlgoBackTest test --projects algo-exercise/getting-started
./mvnw -Dtest=StretchAlgoBackTest test --projects algo-exercise/getting-started

## Usage
-Simulating Market Data: The system simulates market data ticks, which can be customized to create specific trading scenarios.
-Executing Orders: The trading logic will automatically execute orders based on the defined strategies in MyAlgoLogic or StretchAlgoLogic to make profit.
-Monitoring Results: Logs provide detailed information about order states and trading outcomes, which can be analyzed to assess strategy performance.

## Contributing
Contributions are welcome! Please feel free to open issues or submit pull requests for enhancements or bug fixes.

## License
This project is licensed under the MIT License - see the LICENSE file for details.