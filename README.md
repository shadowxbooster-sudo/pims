# Product Inventory Management System (OOPS Project)

This project demonstrates **all OOP concepts taught in lectures L1–L19**, including:

- Classes, Objects  
- Constructors  
- Encapsulation  
- Access Modifiers  
- Methods  
- Inheritance  
- Interfaces  
- Anonymous Classes  
- Inner Classes  
- Comparator & Comparable  
- Collections (ArrayList)  
- Exception Handling  
- Wrapper Classes  

---

## 📦 Features
- Add product  
- Remove product  
- Update quantity  
- Sort by name  
- Sort by price  
- List all products  
- Backend written fully in **Java**  
- Frontend is **HTML + JS**  
- REST API built using built-in Java HTTPServer  

---

## 🚀 Deploy Backend (Java) on Render

1. Go to https://render.com  
2. Click **New → Web Service**  
3. Connect your GitHub repo  
4. Set:
   
Runtime: Java
Build Command: javac Server.java
Start Command: java Server

5. Deploy  

---

## 🌐 Deploy Frontend (HTML) on Netlify
1. Upload only `index.html`
2. That’s it.

---

## 🔗 Connect Frontend to Backend
Edit `API` inside `index.html`:

```js
const API = "https://your-render-backend-url.onrender.com";
