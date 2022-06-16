import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import { w3cwebsocket as W3CWebSocket } from "websocket";

const client = new W3CWebSocket('ws://localhost:8000/ws/game');

function Square(props) {
    return (
        <button className="square" onClick={props.onClick}>
          {props.value}
        </button>
    );
}
  
class Board extends React.Component {
    componentWillMount() {
      
        client.onmessage = (message) => {
          console.log(message);
          let event = JSON.parse(message.data);
          if (event && event.type === "start") {
            this.setState({
              sign: event.sign,
              next: event.next
            });
          } else if (event && event.type === "move") {
            const squares = this.state.squares.slice();
            squares[event.move] = event.player;
            this.setState({
              next: event.next,
              squares: squares
            });
          } else if (event && event.type === "state") {
            this.setState({
              sign: event.sign,
              next: event.next,
              squares: event.squares,
              winner: event.winner
            });
          } else if (event && event.type === "end") {
            this.setState({
              winner: event.winner
            });
          }
        };
    }

    constructor(props) {
        super(props);
        this.state = {
          squares: Array(9).fill(null),
          sign: "",
          next: "",
          winner: "",
          xIsNext: true,
        };
    }
  
    handleClick(i) {
        if (this.state.sign && this.state.sign !== this.state.next) {
            return;
        }
        const squares = this.state.squares.slice();
        if (squares[i] || this.state.winner) {
          return;
        }
        squares[i] = this.state.sign;
        this.setState({
          squares: squares
        });
        client.send(i);
    }
  
    renderSquare(i) {
        return (
          <Square
            value={this.state.squares[i]}
            onClick={() => this.handleClick(i)}
          />
        );
    }
  
    render() {
        let status;
        if (this.state.winner) {
            status = 'Winner: ' + this.state.winner;
        } else {
            status = 'Next player: ' + this.state.next;
        }
    
        return (
            <div>
              <div className="sign">Your sign: {this.state.sign}</div>
              <div className="status">{status}</div>
              <div className="board-row">
                {this.renderSquare(0)}
                {this.renderSquare(1)}
                {this.renderSquare(2)}
              </div>
              <div className="board-row">
                {this.renderSquare(3)}
                {this.renderSquare(4)}
                {this.renderSquare(5)}
              </div>
              <div className="board-row">
                {this.renderSquare(6)}
                {this.renderSquare(7)}
                {this.renderSquare(8)}
              </div>
            </div>
        );
    }
}
  
class Game extends React.Component {
    render() {
        return (
            <div className="game">
              <div className="game-board">
                <Board />
              </div>
            </div>
        );
    }
}

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(<Game />);
