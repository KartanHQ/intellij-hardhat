// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.9;

import "./Game.sol";

contract Stio is Game{
    address payable public owner;

    mapping(address => Account) public accountMap;

    struct Account {
        bool hasOwner;
        uint balance;
    }

    constructor() {
        owner = payable(msg.sender);
    }

    function registerAddress() external {
        require(accountMap[msg.sender].hasOwner == false,"Account already existing");

        accountMap[msg.sender] = Account(true,0);
        emit AccountCreation(msg.sender);
    }

    function deposit() external payable {
        uint oldBalance = accountMap[msg.sender].balance;
        require(msg.value > 0, "Deposit funds cannot be 0");
        require(accountMap[msg.sender].hasOwner == true,"Account not existing");
        require(oldBalance + msg.value > oldBalance,"Balance not larger after deposit");

        accountMap[msg.sender].balance = oldBalance + msg.value;
        emit AccountDeposit(msg.sender,oldBalance,accountMap[msg.sender].balance,msg.value);
    }

    function withdraw(uint amount) external {
        uint oldBalance = accountMap[msg.sender].balance;
        require(amount > 0, "Withdraw funds must be over 0");
        require(accountMap[msg.sender].hasOwner == true,"Account not existing");
        require(oldBalance >= amount,"Not enough balance");
        require(oldBalance - amount < oldBalance,"Balance not smaller after withdraw");

        address payable receiver = payable(msg.sender);

        accountMap[msg.sender].balance = oldBalance - amount;

        require(receiver.send(amount), "Failed to send Ether");

        emit AccountWithdraw(msg.sender,oldBalance,accountMap[msg.sender].balance,amount);
    }
}