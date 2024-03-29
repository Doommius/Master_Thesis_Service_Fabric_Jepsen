﻿// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

namespace JepsenAPIStore.Controllers
{
    using System.Collections.Generic;
    using System.Threading;
    using System.Threading.Tasks;
    using Microsoft.AspNetCore.Mvc;
    using Microsoft.ServiceFabric.Data;
    using Microsoft.ServiceFabric.Data.Collections;
    using System.Net.Http;
    using System;
    using System.Fabric;

    [Route("api/[controller]")]
    public class ReliableConcurrentQueueController : Controller
    {
        private readonly IReliableStateManager StateManager;

        public ReliableConcurrentQueueController(IReliableStateManager stateManager)
        {
            this.StateManager = stateManager;
        }


        // GET api/ReliableConcurrentQueue/
        [HttpGet("")]
        public async Task<IActionResult> Get()
        {
            IReliableConcurrentQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableConcurrentQueue<String>>("myReliableConcurrentQueue");
            using (var txn = this.StateManager.CreateTransaction())
            {
                long returnvalue = queue.Count;

                return this.Json((new KeyValuePair<string, long>("Queue Length", returnvalue)));

            }
        }

        // PUT VoteData/name
        [HttpDelete("")]
        public async Task<IActionResult> get()
        {
            CancellationToken ct = new CancellationToken();

            IReliableConcurrentQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableConcurrentQueue<String>>("myReliableConcurrentQueue");

            using (var txn = this.StateManager.CreateTransaction())
            {
                ConditionalValue<string> returnvalue = await queue.TryDequeueAsync(txn, ct);

                await txn.CommitAsync();

                if (returnvalue.HasValue)
                {
                    return this.Json(new KeyValuePair<string, string>("Dequeue", returnvalue.Value));
                }
                else
                {
                    return new BadRequestResult();
                }
            }



        }
        [HttpPut("{value}")]
        public async Task<IActionResult> put(String value)
        {
            CancellationToken ct = new CancellationToken();

            IReliableConcurrentQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableConcurrentQueue<String>>("myReliableConcurrentQueue");

            using (var txn = this.StateManager.CreateTransaction())
            {
                await queue.EnqueueAsync(txn, value, ct);
                await txn.CommitAsync();


            }

            return new OkResult();



        }


    }
}