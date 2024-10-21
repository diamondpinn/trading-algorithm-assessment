# **UI-Frontend-Assessment**

## **Market Depth Feature**

### **Overview**

This project implements a **Market Depth** feature that displays **real-time bid and ask price levels** along with their respective quantities. It fetches **market data** using the `@vuu-ui` library, processes it through various **React hooks**, and presents the information in an **interactive and dynamic UI**.

---

## **Key Features**

- **Live Market Depth Display**: Shows real-time bid/ask prices and quantities.
- **Dynamic Price Direction Indicators**: Arrows indicate price trends (up, down, or neutral).
- **Quantity Visualization**: Horizontal bars adjust based on quantity changes to visually represent the depth of the market.

---

## **File Structure**

### **Components**

1. **MarketDepthFeature.tsx**
   - The main component responsible for managing and rendering the market data panel.

2. **MarketDataPanel.tsx**
   - A UI panel that toggles between an **instruction view** and the **market depth table**.
   - Contains a button to switch between these viewing modes.

3. **MarketTable.tsx**
   - Displays the market depth table with **bid and ask prices** and corresponding **quantities**.
   - Implements **price direction arrows** to indicate if the price is moving **up**, **down**, or staying **neutral**.
   - Uses **horizontal bars** to show the relative size of the bids and offers.

4. **PriceCell.tsx**
   - Displays **bid/ask prices** and the directional arrows that show if the price has **increased**, **decreased**, or remained **neutral**.

5. **QuantityCell.tsx**
   - Displays **bid/ask quantities** and dynamically adjusts the width of the bar to visually represent changes in quantity.

---

## **Hooks Used**

This project uses several **React hooks**, each designed to manage **component state**, handle **side effects**, and improve **performance**.

### **1. useState**
- **Purpose**: To manage component state, such as prices and quantities.
- **Example**: 
  - `MarketDataPanel.tsx` uses `useState` to toggle between **instruction** and **design** views.
  - Both `PriceCell` and `QuantityCell` use `useState` to store the **current price** or **quantity**, as well as their **previous values**, in order to detect changes.

### **2. useEffect**
- **Purpose**: To handle **side effects**, such as fetching data or updating UI based on changes.
- **Example**: 
  - In `MarketTable.tsx`, `useEffect` is used to track previous bid and offer prices. Each time the `data` prop changes, it compares the current and previous prices to update the directional arrows.

  ```jsx
  useEffect(() => {
    data.forEach((item) => {
      previousPrices.current[item.symbolLevel] = {
        bid: item.bid,
        offer: item.offer,
      };
    });
  }, [data]);
### **3. useRef**
Purpose: To persist values across renders without triggering re-renders. Useful for tracking previous prices or values.
### **4. useCallback**
Purpose: To memoize functions to prevent them from being recreated on every render.

### **5. useMarketDepthData**
Purpose: A custom hook that fetches live market depth data from the @vuu-ui data source.
**Example:**
-useMarketDepthData is used in MarketDepthFeature.tsx to subscribe to live data feeds and pass the data into components like MarketTable.

### **Styling**
- MarketDataPanel.css
Contains styles for the layout of the market data panel, including background colors, padding, and button styles.
- MarketTable.css
Defines the table styles for bid/ask prices and quantities, focusing on cell alignment, row highlights, and rendering directional arrows.
- PriceCell.css
Provides styles for the price cells, including the arrow icons that show price direction (up, down, or neutral).
- QuantityCell.css
Styles the quantity bars for bid/ask values, adjusting their size and color based on the changes in quantities.
### **Running the Project**
**Prerequisites**
- Node.js should be installed on your machine.
- All required dependencies are listed in the package.json file.

**Setup Instructions**
- Clone the repository:

**bash**

**git clone <repository-url>**
- Navigate to the project directory:

**bash**

cd <project-directory>

**Install the dependencies:**

**bash**

**npm install**

- Start the development server:

**bash**

**npm run dev**
- Open your browser and navigate to http://localhost:3000 to view the application.

**Running Tests**
To run tests for the project, use the following command:

**bash**
**npm test**
- This will run unit tests and check component functionality. Ensure that you have implemented tests for major components and hooks.

