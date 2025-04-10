import { useState } from 'react'
import axios from 'axios'
import './App.css'

function App() {
  const [code, setCode] = useState('')
  const [vars, setVars] = useState(["", "", "", ""])
  const [output, setOutput] = useState('')
  const [isRunning, setIsRunning] = useState(false)

  const runCode = async () => {
    setIsRunning(true)
    setOutput('⏳ Running your code...')
    try {
      const response = await axios.post('http://localhost:8080/api/run', {
        code,
        vars
      })
      setOutput(response.data)
    } catch (error) {
      setOutput(error.response?.data || "⚠️ Error: Backend is down or not reachable")
    } finally {
      setIsRunning(false)
    }
  }

  return (
    <div className="app">
      <h1>⚙️ Java Code Runner</h1>

      <div className="editor-container">
        <div className="code-section">
          <div className="section-header">
            <label>Java Code:</label>
            <button 
              className="run-button" 
              onClick={runCode}
              disabled={isRunning}
            >
              {isRunning ? 'Running...' : '▶ Run Code'}
            </button>
          </div>
          
          <textarea
            className="code-input"
            placeholder="Enter Java code here..."
            value={code}
            onChange={(e) => setCode(e.target.value)}
          />

          <div className="inputs">
            {vars.map((val, i) => (
              <input
                key={i}
                placeholder={`Variable ${i + 1}`}
                value={val}
                onChange={(e) => {
                  const newVars = [...vars]
                  newVars[i] = e.target.value
                  setVars(newVars)
                }}
              />
            ))}
          </div>
        </div>

        <div className="output-section">
          <div className="section-header">
            <label>Output:</label>
          </div>
          <pre className="output-box">
            {output}
          </pre>
        </div>
      </div>
    </div>
  )
}

export default App